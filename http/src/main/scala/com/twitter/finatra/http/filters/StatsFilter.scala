package com.twitter.finatra.http.filters

import com.twitter.finagle.http.{Method => HttpMethod, Request, Response, Status}
import com.twitter.finagle.stats.{Counter, Stat, StatsReceiver}
import com.twitter.finagle.{Service, SimpleFilter}
import com.twitter.finatra.http.contexts.RouteInfo
import com.twitter.finatra.http.filters.StatsFilter.Stats
import com.twitter.finatra.http.response.SimpleResponse
import com.twitter.inject.Logging
import com.twitter.util.{Duration, Future, Memoize, Return, Stopwatch, Throw}
import javax.inject.{Inject, Singleton}

object StatsFilter {
  private object Stats {
    // Unless stats are per-endpoint, don't track request count/time
    // since those are handled by [[com.twitter.finagle.service.StatsFilter]]
    // already.
    def mk(statsReceiver: StatsReceiver, statusCode: Int, perEndpoint: Boolean): Stats = {
      val statusClass = s"${statusCode / 100}XX"
      Stats(
        requestCount = if (perEndpoint) Some(statsReceiver.counter("requests")) else None,
        statusCodeCount = statsReceiver.scope("status").counter(statusCode.toString),
        statusClassCount = statsReceiver.scope("status").counter(statusClass),
        requestTime = if (perEndpoint) Some(statsReceiver.stat("time")) else None,
        statusCodeTime = statsReceiver.scope("time").stat(statusCode.toString),
        statusClassTime = statsReceiver.scope("time").stat(statusClass),
        responseSize = statsReceiver.stat("response_size"))
    }
  }

  private case class Stats(
    requestCount: Option[Counter],
    statusCodeCount: Counter,
    statusClassCount: Counter,
    requestTime: Option[Stat],
    statusCodeTime: Stat,
    statusClassTime: Stat,
    responseSize: Stat) {
    def count(duration: Duration, response: Response): Unit = {
      requestCount.foreach { _.incr() }
      statusCodeCount.incr()
      statusClassCount.incr()

      val durationMs = duration.inMilliseconds
      requestTime.foreach { _.add(durationMs.toFloat) }
      statusCodeTime.add(durationMs.toFloat)
      statusClassTime.add(durationMs.toFloat)

      responseSize.add(response.length.toFloat)
    }
  }
}

/**
 * A drop-in replacement for [[com.twitter.finagle.http.filter.StatsFilter]]
 * with per-route stats scoped under `route/<name>/<method>`.
 */
@Singleton
class StatsFilter[R <: Request] @Inject()(
  statsReceiver: StatsReceiver)
  extends SimpleFilter[R, Response]
  with Logging {

  private val perRouteStats = Memoize[(RouteInfo, HttpMethod, Int), Stats] {
    case (routeInfo, method, statusCode) =>

      val nameOrPath = 
        if (routeInfo.name.nonEmpty)
          routeInfo.name
        else
          routeInfo.sanitizedPath

      val scopedStatsReceiver =
        statsReceiver.
          scope("route").
          scope(nameOrPath).
          scope(method.toString.toUpperCase)
      Stats.mk(scopedStatsReceiver, statusCode, perEndpoint = true)
  }

  private val globalStats = Memoize[Int, Stats] { statusCode =>
    Stats.mk(statsReceiver, statusCode, perEndpoint = false)
  }

  /* Public */

  def apply(request: R, service: Service[R, Response]): Future[Response] = {
    val elapsed = Stopwatch.start()
    service(request) respond {
      case Return(response) =>
        record(request, response, elapsed())
      case Throw(e) =>
        error(s"Internal server error - please ensure ${classOf[ExceptionMappingFilter[_]].getName} is installed", e)
        record(request, SimpleResponse(Status.InternalServerError), elapsed())
    }
  }

  /* Private */

  private def record(request: Request, response: Response, duration: Duration): Unit = {
    globalStats(response.statusCode).count(duration, response)
    RouteInfo(request) foreach { routeInfo =>
      perRouteStats((routeInfo, request.method, response.statusCode)).count(duration, response)
    }
  }
}
