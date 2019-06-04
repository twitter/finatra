package com.twitter.finatra.thrift.filters

import com.twitter.finagle.{Filter, Service}
import com.twitter.finagle.service.{ReqRep, ResponseClass, ResponseClassifier}
import com.twitter.finagle.stats.Stat.timeFuture
import com.twitter.finagle.stats.{Counter, Stat, StatsReceiver}
import com.twitter.finagle.thrift.MethodMetadata
import com.twitter.finatra.thrift.response.ThriftResponseClassifier
import com.twitter.inject.Logging
import com.twitter.util.{Future, Memoize, Throw, Try}
import javax.inject.{Inject, Singleton}

private object StatsFilter {
  /** INTENDED FOR INTERNAL USE ONLY */
  object ThriftMethodStats {
    def apply(stats: StatsReceiver): ThriftMethodStats =
      ThriftMethodStats(
        latencyStat = stats.stat("latency_ms"),
        successCounter = stats.counter("success"),
        failuresCounter = stats.counter("failures"),
        failuresScope = stats.scope("failures"),
        successesScope = stats.scope("success")
      )
  }

  /** INTENDED FOR INTERNAL USE ONLY */
  case class ThriftMethodStats(
    latencyStat: Stat,
    successCounter: Counter,
    failuresCounter: Counter,
    failuresScope: StatsReceiver,
    successesScope: StatsReceiver
  )
}

/**
 * Tracks "per method" statistics scoped under `per_method_stats/<method>` including:
 *  - success/failure (with exceptions) counters
 *  - latency_ms histogram
 *
 * Example stats for a successful request to a method named `foo`:
 *
 * {{{
 *   per_method_stats/foo/failures 0
 *   per_method_stats/foo/success 1
 *   per_method_stats/foo/latency_ms 43.000000 [43.0]
 * }}}
 *
 * Example stats, for a failed request to a method named `foo`:
 *
 * {{{
 *   exceptions 1
 *   exceptions/java.lang.Exception 1
 *   per_method_stats/foo/failures 1
 *   per_method_stats/foo/failures/java.lang.Exception 1
 *   per_method_stats/foo/success 0
 *   per_method_stats/foo/latency_ms 43.000000 [43.0]
 * }}}
 *
 *
 * @note It is expected that this Filter is inserted ABOVE the [[ExceptionMappingFilter]] in a
 *       given filter chain, e.g., `StatsFilter.andThen(ExceptionMappingFilter)`.
 *       For the response flow, [[StatsFilter]] would happen AFTER [[ExceptionMappingFilter]] and
 *       calculate mapped result.
 *
 * @param statsReceiver      the [[com.twitter.finagle.stats.StatsReceiver]] to which
 *                           to record stats.
 * @param responseClassifier a [[ThriftResponseClassifier]] used to determine when a response
 *                           is successful or not.
 */
@Singleton
class StatsFilter @Inject()(
  statsReceiver: StatsReceiver,
  responseClassifier: ThriftResponseClassifier
) extends Filter.TypeAgnostic
  with Logging {

  import StatsFilter._

  /* Public */

  /**
   * Secondary constructor which accepts a [[StatsReceiver]]. The [[ThriftResponseClassifier]]
   * is defaulted to [[ThriftResponseClassifier.ThriftExceptionsAsFailures]].
   *
   * @param statsReceiver the [[com.twitter.finagle.stats.StatsReceiver]] to which
   *                      to record stats.
   */
  def this(statsReceiver: StatsReceiver) {
    this(statsReceiver, ThriftResponseClassifier.ThriftExceptionsAsFailures)
  }

  def toFilter[T, U]: Filter[T, U, T, U] = new Filter[T, U, T, U] {

    private[this] val requestStats = statsReceiver.scope("per_method_stats")
    private[this] val exceptionCounter = statsReceiver.counter("exceptions")
    private[this] val exceptionStatsReceiver = statsReceiver.scope("exceptions")

    private[this] val perMethodStats = Memoize { methodName: String =>
      ThriftMethodStats(requestStats.scope(methodName))
    }

    /**
     * The application of the [[ResponseClassifier]] differs from the Finagle default. This class attempts
     * to preserve information in the emitted metrics. That is, if an exception is returned, even if it
     * is classified as a "success", we incr the exception counter(s) (in addition to the "success"
     * or "failures" counters). Conversely, if a response (non-exception) is returned which is classified
     * as a "failure", we incr the "failures" counter but we do not incr any exception counter. Finally,
     * responses or exceptions classified as "ignorable" only increment the exception counter(s).
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
     *  |   IGNORABLE    | (no-op)         | exc.incr()                |
     *  *----------------*-----------------*---------------------------*
     * }}}
     *
     * @see [[com.twitter.finagle.service.StatsFilter]]
     * @see [[com.twitter.finagle.service.ResponseClassifier]]
     */
    def apply(
      request: T,
      service: Service[T, U]
    ): Future[U] = {
      val stats: Option[ThriftMethodStats] = MethodMetadata.current.map(m => perMethodStats(m.methodName))

      executeRequest(stats, request, service).respond { response =>
        handleResponse(stats, request, response)
      }
    }

    /* Private */

    private[this] def executeRequest(
      stats: Option[ThriftMethodStats],
      request: T,
      service: Service[T, U]): Future[U] = {

      stats
        .map(perMethodStats => timeFuture(perMethodStats.latencyStat)(service(request)))
        .getOrElse(service(request))
    }

    private[this] def handleResponse(
      stats: Option[ThriftMethodStats],
      request: T,
      response: Try[U]): Unit = {
      responseClassifier.applyOrElse(
        ReqRep(request, response),
        ResponseClassifier.Default
      ) match {
        case ResponseClass.Ignorable =>
          countExceptions(response)
        case ResponseClass.Failed(_) =>
          stats.foreach(_.failuresCounter.incr())
          countExceptions(response)
          countPerMethodStats(stats, success = false, response)
        case ResponseClass.Successful(_) =>
          stats.foreach(_.successCounter.incr())
          countExceptions(response)
          countPerMethodStats(stats, success = true, response)
      }
    }

    private[this] def countExceptions(response: Try[_]): Unit = response match {
      case Throw(e) =>
        exceptionCounter.incr()
        exceptionStatsReceiver.counter(e.getClass.getName).incr()
      case _ =>
        // do nothing
    }

    private[this] def countPerMethodStats(
      stats: Option[ThriftMethodStats],
      success: Boolean,
      response: Try[_]): Unit = response match {
      case Throw(e) =>
        stats.foreach { perMethodStats =>
          if (success) perMethodStats.successesScope.counter(e.getClass.getName).incr()
          else perMethodStats.failuresScope.counter(e.getClass.getName).incr()
        }
      case _ =>
        // do nothing
    }
  }
}
