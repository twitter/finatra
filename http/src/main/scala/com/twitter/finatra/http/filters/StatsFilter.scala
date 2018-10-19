package com.twitter.finatra.http.filters

import com.twitter.finagle.http.{Request, Response, Status, Method => HttpMethod}
import com.twitter.finagle.service.{ReqRep, ResponseClass, ResponseClassifier}
import com.twitter.finagle.stats.{Counter, Stat, StatsReceiver}
import com.twitter.finagle.{Service, SimpleFilter}
import com.twitter.finatra.http.contexts.RouteInfo
import com.twitter.finatra.http.response.{HttpResponseClassifier, SimpleResponse}
import com.twitter.inject.Logging
import com.twitter.util.{Duration, Future, Memoize, Return, Stopwatch, Throw}
import javax.inject.{Inject, Singleton}

private object StatsFilter {
  val url = "https://twitter.github.io/finatra/user-guide/http/exceptions.html#exceptionmappingfilter"

  /** INTENDED FOR INTERNAL USE ONLY */
  object Stats {
    def mk(statsReceiver: StatsReceiver, statusCode: Int, perEndpoint: Boolean): Stats = {
      val statusClass = s"${statusCode / 100}XX"
      Stats(
        requestCount = if (perEndpoint) Some(statsReceiver.counter("requests")) else None,
        statusCodeCount = statsReceiver.scope("status").counter(statusCode.toString),
        statusClassCount = statsReceiver.scope("status").counter(statusClass),
        requestTime = if (perEndpoint) Some(statsReceiver.stat("time")) else None,
        statusCodeTime = statsReceiver.scope("time").stat(statusCode.toString),
        statusClassTime = statsReceiver.scope("time").stat(statusClass),
        responseSize = statsReceiver.stat("response_size"),
        successCount = if (perEndpoint) Some(statsReceiver.counter("success")) else None,
        failuresCount = if (perEndpoint) Some(statsReceiver.counter("failures")) else None
      )
    }
  }

  /** INTENDED FOR INTERNAL USE ONLY */
  case class Stats(
    requestCount: Option[Counter],
    statusCodeCount: Counter,
    statusClassCount: Counter,
    requestTime: Option[Stat],
    statusCodeTime: Stat,
    statusClassTime: Stat,
    responseSize: Stat,
    successCount: Option[Counter],
    failuresCount: Option[Counter]
  ) {
    def count(duration: Duration, response: Response, success: Boolean): Unit = {
      requestCount.foreach(_.incr())
      statusCodeCount.incr()
      statusClassCount.incr()

      val durationMs = duration.inMilliseconds
      requestTime.foreach(_.add(durationMs.toFloat))
      statusCodeTime.add(durationMs.toFloat)
      statusClassTime.add(durationMs.toFloat)

      responseSize.add(response.length.toFloat)
      if (success) {
        successCount.foreach(_.incr())
      } else {
        failuresCount.foreach(_.incr())
      }
    }
  }
}

/**
 * A drop-in replacement for [[com.twitter.finagle.http.filter.StatsFilter]]
 * with per-route stats scoped under `route/<name>/<method>`.
 *
 * Example stats for a successful GET request to a route named `/foo`:
 *
 * {{{
 *   route/foo/GET/failures 0
 *   route/foo/GET/requests 1
 *   route/foo/GET/status/200 1
 *   route/foo/GET/status/2XX 1
 *   route/foo/GET/success 1
 *   route/foo/GET/response_size 13.000000 [13.0]
 *   route/foo/GET/time 857.000000 [857.0]
 *   route/foo/GET/time/200 857.000000 [857.0]
 *   route/foo/GET/time/2XX 857.000000 [857.0]
 *   status/200 1
 *   status/2XX 1
 *   response_size 13.000000 [13.0]
 *   time/200 857.000000 [857.0]
 *   time/2XX 857.000000 [857.0]
 * }}}
 *
 * Example stats for a failed GET request to a route named `/foo`:
 *
 * {{{
 *   route/foo/GET/failures 1
 *   route/foo/GET/requests 1
 *   route/foo/GET/status/500 1
 *   route/foo/GET/status/5XX 1
 *   route/foo/GET/success 0
 *   route/foo/GET/response_size 0.000000 [0.0]
 *   route/foo/GET/time 86.000000 [86.0]
 *   route/foo/GET/time/500 86.000000 [86.0]
 *   route/foo/GET/time/5XX 86.000000 [86.0]
 *   status/500 1
 *   status/5XX 1
 *   response_size 0.000000 [0.0]
 *   time/500 86.000000 [86.0]
 *   time/5XX 86.000000 [86.0]
 * }}}
 *
 * @note It is expected that this Filter occurs "BEFORE" the [[ExceptionMappingFilter]] in a
 *       given filter chain, e.g., `StatsFilter.andThen(ExceptionMappingFilter)`. It is expected
 *       that there SHOULD be a returned response because the [[ExceptionMappingFilter]] should
 *       return give a non-exception response.
 *
 * @see [[com.twitter.finagle.http.filter.StatsFilter Finagle HTTP StatsFilter]]
 *
 * @param statsReceiver      the [[com.twitter.finagle.stats.StatsReceiver]] to which
 *                           to record stats.
 * @param responseClassifier an [[HttpResponseClassifier]] used to determine when a response
 *                           is successful or not.
 * @tparam R the type of the [[StatsFilter]] which is upper bounded by the
 *           [[com.twitter.finagle.http.Request]] type.
 */
@Singleton
class StatsFilter[R <: Request] @Inject()(
  statsReceiver: StatsReceiver,
  responseClassifier: HttpResponseClassifier
) extends SimpleFilter[R, Response]
  with Logging {

  import StatsFilter._

  private[this] val perRouteStats = Memoize[(RouteInfo, HttpMethod, Int), Stats] {
    case (routeInfo, method, statusCode) =>
      val nameOrPath =
        if (routeInfo.name.nonEmpty)
          routeInfo.name
        else
          routeInfo.sanitizedPath

      val scopedStatsReceiver =
        statsReceiver.scope("route").scope(nameOrPath).scope(method.toString.toUpperCase)
      Stats.mk(scopedStatsReceiver, statusCode, perEndpoint = true)
  }

  private[this] val globalStats = Memoize[Int, Stats] { statusCode =>
    Stats.mk(statsReceiver, statusCode, perEndpoint = false)
  }

  /* Public */

  /**
   * Secondary constructor which accepts a [[StatsReceiver]]. The [[HttpResponseClassifier]] is
   * defaulted to [[HttpResponseClassifier.ServerErrorsAsFailures]].
   *
   * @param statsReceiver the [[com.twitter.finagle.stats.StatsReceiver]] to which
   *                      to record stats.
   */
  def this(statsReceiver: StatsReceiver) {
    this(statsReceiver, HttpResponseClassifier.ServerErrorsAsFailures)
  }

  /**
   * The application of the [[ResponseClassifier]] differs from the Finagle default. This class attempts
   * to preserve information in the emitted metrics. That is, if an exception is returned, even if it
   * is classified as a "success", we incr the the exception counter(s) (in addition to the "success"
   * or "failures" counters). Conversely, if a response (non-exception) is returned which is classified
   * as a "failure", we incr the "failures" counter but we do not incr any exception counter.
   *
   * {{{
   *                   *-----------------*---------------------------*
   *                   |              Returned Response              |
   *  *----------------*-----------------*---------------------------*
   *  | Classification |    RESPONSE     |        EXCEPTION          |
   *  *----------------*-----------------*---------------------------*
   *  |  SUCCESSFUL    | success.incr()  | success.incr(), exc.incr()|
   *  *----------------*-----------------*---------------------------*
   *  |    FAILED      | failed.incr()   | failed.incr(), exc.incr() |
   *  *----------------*-----------------*---------------------------*
   * }}}
   *
   * @see [[com.twitter.finagle.service.StatsFilter]]
   * @see [[com.twitter.finagle.service.ResponseClassifier]]
   */
  def apply(request: R, service: Service[R, Response]): Future[Response] = {
    val elapsed = Stopwatch.start()
    service(request).respond { response =>
      val success = responseClassifier.applyOrElse(
        ReqRep(request, response),
        ResponseClassifier.Default
      ) match {
        case ResponseClass.Failed(_) => false
        case ResponseClass.Successful(_) => true
      }

      response match {
        case Throw(e) =>
          warn(
            s"Uncaught exception: ${e.getClass.getName}. " +
              s"Please ensure ${classOf[ExceptionMappingFilter[_]].getName} is installed. " +
              s"For more details see: $url"
          )
          // Treat exceptions as empty 500 errors
          count(elapsed(), request, SimpleResponse(Status.InternalServerError), success = success)
        case Return(rep) =>
          count(elapsed(), request, rep, success = success)
      }
    }
  }

  /* Private */

  private def count(
    duration: Duration,
    request: Request,
    response: Response,
    success: Boolean): Unit = {
    globalStats(response.statusCode).count(duration, response, success)
    RouteInfo(request).foreach { routeInfo =>
      perRouteStats((routeInfo, request.method, response.statusCode))
        .count(duration, response, success)
    }
  }
}
