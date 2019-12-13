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
        failuresCounter = stats.counter("failures"),
        failuresScope = stats.scope("failures"),
        ignoredCounter = stats.counter("ignored"),
        ignoredScope = stats.scope("ignored"),
        latencyStat = stats.stat("latency_ms"),
        successCounter = stats.counter("success"),
        successesScope = stats.scope("success"),
        requestsCounter = stats.counter("requests")
      )
  }

  /** INTENDED FOR INTERNAL USE ONLY */
  case class ThriftMethodStats(
    failuresCounter: Counter,
    failuresScope: StatsReceiver,
    ignoredCounter: Counter,
    ignoredScope: StatsReceiver,
    latencyStat: Stat,
    successCounter: Counter,
    successesScope: StatsReceiver,
    requestsCounter: Counter)
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
 *   per_method_stats/foo/ignored 0
 *   per_method_stats/foo/requests 1
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
 *   per_method_stats/foo/ignored 0
 *   per_method_stats/foo/requests 1
 *   per_method_stats/foo/success 0
 *   per_method_stats/foo/latency_ms 43.000000 [43.0]
 * }}}
 *
 * Example stats, for a failed request to a method named `foo` with a [[ResponseClassifier]] which
 * classifies [[java.lang.Exception]] as Ignorable:
 *
 * {{{
 *   exceptions 1
 *   exceptions/java.lang.Exception 1
 *   per_method_stats/foo/failures 0
 *   per_method_stats/foo/ignored 1
 *   per_method_stats/foo/ignored/java.lang.Exception 1
 *   per_method_stats/foo/requests 1
 *   per_method_stats/foo/success 0
 *   per_method_stats/foo/latency_ms 43.000000 [43.0]
 * }}}
 *
 * requests == success + failures + ignored
 * The logical success rate for a method can be calculated as `success / (success + failures)`
 *
 * @note It is expected that this Filter is inserted ABOVE the [[ExceptionMappingFilter]] in a
 *       given filter chain, e.g., `StatsFilter.andThen(ExceptionMappingFilter)`.
 *       For the response flow, [[StatsFilter]] would happen AFTER [[ExceptionMappingFilter]] and
 *       calculate mapped result.
 * @param statsReceiver      the [[com.twitter.finagle.stats.StatsReceiver]] to which
 *                           to record stats.
 * @param responseClassifier a [[ThriftResponseClassifier]] used to determine when a response
 *                           is successful or not.
 */
@Singleton
class StatsFilter @Inject()(
  statsReceiver: StatsReceiver,
  responseClassifier: ThriftResponseClassifier)
    extends Filter.TypeAgnostic
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
     * for responses or exceptions classified as "ignorable", we incr the "ignored" counter and
     * the exception counter(s).
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
     *  |   IGNORABLE    | (no-op)         | ignored.incr(), exc.incr()|
     *  *----------------*-----------------*---------------------------*
     * }}}
     *
     * @see [[com.twitter.finagle.service.StatsFilter]]
     * @see [[com.twitter.finagle.service.ResponseClassifier]]
     */
    def apply(request: T, service: Service[T, U]): Future[U] = {
      val stats: Option[ThriftMethodStats] =
        MethodMetadata.current.map(m => perMethodStats(m.methodName))

      executeRequest(stats, request, service).respond { response =>
        handleResponse(stats, request, response)
      }
    }

    /* Private */

    private[this] def executeRequest(
      stats: Option[ThriftMethodStats],
      request: T,
      service: Service[T, U]
    ): Future[U] = {

      stats
        .map(perMethodStats => timeFuture(perMethodStats.latencyStat)(service(request)))
        .getOrElse(service(request))
    }

    private[this] def handleResponse(
      stats: Option[ThriftMethodStats],
      request: T,
      response: Try[U]
    ): Unit = {
      stats.foreach(_.requestsCounter.incr())
      val responseClass = responseClassifier.applyOrElse(
        ReqRep(request, response),
        ResponseClassifier.Default
      )
      responseClass match {
        case ResponseClass.Ignorable =>
          stats.foreach(_.ignoredCounter.incr())
          countExceptions(response)
          countPerMethodStats(stats, responseClass = responseClass, response)
        case ResponseClass.Failed(_) =>
          stats.foreach(_.failuresCounter.incr())
          countExceptions(response)
          countPerMethodStats(stats, responseClass = responseClass, response)
        case ResponseClass.Successful(_) =>
          stats.foreach(_.successCounter.incr())
          countExceptions(response)
          countPerMethodStats(stats, responseClass = responseClass, response)
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
      responseClass: ResponseClass,
      response: Try[_]
    ): Unit = response match {
      case Throw(e) =>
        stats.foreach { perMethodStats =>
          responseClass match {
            case ResponseClass.Successful(_) =>
              perMethodStats.successesScope.counter(e.getClass.getName).incr()
            case ResponseClass.Failed(_) =>
              perMethodStats.failuresScope.counter(e.getClass.getName).incr()
            case ResponseClass.Ignorable =>
              perMethodStats.ignoredScope.counter(e.getClass.getName).incr()
          }
        }
      case _ =>
      // do nothing
    }
  }
}
