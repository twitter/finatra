package com.twitter.inject.thrift.filters

import com.twitter
import com.twitter.concurrent.AsyncSemaphore
import com.twitter.finagle
import com.twitter.finagle._
import com.twitter.finagle.filter.RequestSemaphoreFilter
import com.twitter.finagle.param.HighResTimer
import com.twitter.finagle.service.Backoff._
import com.twitter.finagle.service.Retries.Budget
import com.twitter.finagle.service.RetryPolicy._
import com.twitter.finagle.service._
import com.twitter.finagle.stats.StatsReceiver
import com.twitter.finagle.util.DefaultTimer
import com.twitter.inject.thrift.AndThenService
import com.twitter.inject.thrift.internal.filters.{
  IncrementCounterFilter,
  LatencyFilter,
  ThriftClientExceptionFilter
}
import com.twitter.inject.thrift.utils.ThriftMethodUtils
import com.twitter.inject.utils.ExceptionUtils._
import com.twitter.inject.{Injector, Logging}
import com.twitter.scrooge.{ThriftMethod, ThriftStruct}
import com.twitter.util.tunable.Tunable
import com.twitter.util.{Try, Duration}
import java.util.concurrent.TimeUnit

/**
 * A [[com.twitter.finagle.Filter]] chain builder which provides helper functions for installing and
 * configuring common filters.
 *
 * Filters configured via the helper methods, e.g., [[withRetryPolicy]], [[withTimeout]], [[withRequestTimeout]],
 * [[withMethodLatency]], [[withExceptionFilter]], will be composed in a specific order, (from top-down, assuming requests/responses
 * enter and exit through the top):
 *
 * +------------------------+
 * | latencyFilter          |
 * +------------------------+
 * | exceptionFilter        |
 * +------------------------+
 * | timeoutFilter          |
 * +------------------------+
 * | retryFilter            |
 * +------------------------+
 * | requestTimeoutFilter   |
 * +------------------------+
 * | concurrencyLimitFilter |
 * +------------------------+
 * | filterChain            |
 * +------------------------+
 *
 * where, the `filterChain` is the chain of filters added in the order of calls to [[filtered]].
 *
 * @param injector the [[com.twitter.inject.Injector]]
 * @param statsReceiver a [[com.twitter.finagle.stats.StatsReceiver]]
 * @param clientLabel the thrift client label
 * @param budget the configured [[com.twitter.finagle.service.RetryBudget]]
 * @param method the [[com.twitter.scrooge.ThriftMethod]] for which this filter chain is configured
 * @param timeoutMultiplier a multiplier to apply to timeouts used in any configures [[com.twitter.finagle.service.TimeoutFilter]]
 * @param retryMultiplier a multiplier to apply to the number of retries used in any configured [[com.twitter.finagle.service.RetryFilter]]
 * @param useHighResTimerForRetries if a high resolution [[com.twitter.util.Timer]] should be used such that retries are run tighter to their schedule
 * @param andThenService the [[com.twitter.finagle.Service]] to invoke after the filter chain
 * @tparam Req Request type for this filter chain
 * @tparam Rep Response type for this filter chain
 * @see [[com.twitter.inject.thrift.filters.ThriftClientFilterBuilder]]
 * @see [[com.twitter.finagle.thrift.service.ThriftServicePerEndpoint]]
 */
@deprecated("Use ThriftMethodBuilderClientModule and ThriftMethodBuilder", "2018-01-12")
class ThriftClientFilterChain[Req <: ThriftStruct, Rep](
  injector: Injector,
  statsReceiver: StatsReceiver,
  clientLabel: String,
  budget: Budget,
  method: ThriftMethod,
  timeoutMultiplier: Int,
  retryMultiplier: Int,
  useHighResTimerForRetries: Boolean,
  andThenService: AndThenService
) extends Logging {

  private val retryTimer = {
    if (useHighResTimerForRetries)
      HighResTimer.Default
    else
      finagle.param.Timer.param.default.timer
  }

  private val scopedStatsReceiver = scopeStatsReceiver()

  /** @see [[com.twitter.finagle.thrift.service.ThriftServicePerEndpoint#statsFilter]] */
  // method invocations - incremented every time we call/invoke the method.
  /** Example scope: clnt/thrift/Adder/add1String/method/invocations */
  private val invocationsCounter = scopedStatsReceiver.counter("invocations")

  // Mutable
  protected var filterChain: Filter[Req, Rep, Req, Rep] = Filter.identity

  protected var methodLatencyFilter: Filter[Req, Rep, Req, Rep] = Filter.identity
  protected var exceptionFilterImpl: Filter[Req, Rep, Req, Rep] =
    new ThriftClientExceptionFilter[Req, Rep](clientLabel, method)
  protected var timeoutFilter: Filter[Req, Rep, Req, Rep] = Filter.identity
  protected var retryFilter: Filter[Req, Rep, Req, Rep] = Filter.identity
  protected var requestLatencyFilter: Filter[Req, Rep, Req, Rep] = Filter.identity
  protected var requestTimeoutFilter: Filter[Req, Rep, Req, Rep] = Filter.identity
  protected var concurrencyLimitFilter: Filter[Req, Rep, Req, Rep] = Filter.identity

  /* Public */

  /**
   * Install a [[com.twitter.finagle.Filter]]. This filter will be added to the end of the filter chain. That is, this
   * filter will be invoked AFTER any other installed filter on a request [[Req]] and thus BEFORE any other installed
   * filter on a response [[Rep]].
   *
   * @param filter the [[com.twitter.finagle.Filter]] to install.
   * @return [[ThriftClientFilterChain]]
   */
  def filtered(filter: Filter[Req, Rep, Req, Rep]): ThriftClientFilterChain[Req, Rep] = {
    filterChain = filterChain andThen filter
    this
  }

  /**
   * Install a [[com.twitter.finagle.Filter]]. This filter will be added to the end of the filter chain. That is, this
   * filter will be invoked AFTER any other installed filter on a request [[Req]] and thus BEFORE any other installed
   * filter on a response [[Rep]].
   *
   * @tparam T the type of the filter to instantiate from the injector
   * @return [[ThriftClientFilterChain]]
   */
  def filtered[T <: Filter[Req, Rep, Req, Rep]: Manifest]: ThriftClientFilterChain[Req, Rep] = {
    filtered(injector.instance[T])
  }

  /**
   * Install a [[com.twitter.finagle.Filter]] that is agnostic to the [[ThriftMethod]] Req/Rep types. This allows for
   * use of more general filters that do not care about the [[ThriftMethod]] input and output types.
   *
   * @param filter the [[com.twitter.finagle.Filter.TypeAgnostic]] to install.
   * @return [[ThriftClientFilterChain]]
   */
  def withAgnosticFilter(filter: Filter.TypeAgnostic): ThriftClientFilterChain[Req, Rep] = {
    filterChain = filterChain.andThen(filter.toFilter[Req, Rep])
    this
  }

  /**
   * Install a [[com.twitter.finagle.Filter.TypeAgnostic]] that is agnostic to the [[ThriftMethod]] Req/Rep types. This allows for
   * use of more general filters that do not care about the [[ThriftMethod]] input and output types.
   *
   * @tparam T the type of the filter to instantiate from the injector
   * @return [[ThriftClientFilterChain]]
   */
  def withAgnosticFilter[T <: Filter.TypeAgnostic: Manifest]: ThriftClientFilterChain[Req, Rep] = {
    withAgnosticFilter(injector.instance[T])
  }

  /**
   * Install a [[com.twitter.finagle.Filter]] specific to handling exceptions. This filter will be correctly positioned
   * in the filter chain near the top of the stack. This filter is generally used to mutate or alter the final response
   * [[Req]] based on a returned exception. E.g., to translate a transport-level exception from Finagle to an
   * application-level exception.
   *
   * @param filter the [[com.twitter.finagle.Filter]] to install.
   * @return [[ThriftClientFilterChain]]
   */
  def withExceptionFilter(filter: Filter[Req, Rep, Req, Rep]): ThriftClientFilterChain[Req, Rep] = {
    exceptionFilterImpl = filter
    this
  }

  /**
   * Install a [[com.twitter.finagle.Filter]] specific to handling exceptions. This filter will be correctly positioned
   * in the filter chain near the top of the stack. This filter is generally used to mutate or alter the final response
   * [[Req]] based on a returned exception. E.g., to translate a transport-level exception from Finagle to an
   * application-level exception.
   *
   * @tparam T the type of the filter to instantiate from the injector
   * @return [[ThriftClientFilterChain]]
   */
  def withExceptionFilter[T <: Filter[Req, Rep, Req, Rep]: Manifest]
    : ThriftClientFilterChain[Req, Rep] = {
    withExceptionFilter(injector.instance[T])
  }

  /**
   * Install a [[com.twitter.finagle.service.RetryFilter]] configured with a [[com.twitter.finagle.service.RetryPolicy]]
   * using constant backoffs.
   *
   * @param shouldRetry a PartialFunction over the both the [[Req]] and a Try-wrapped returned [[Rep]]. Only one of
   *                    `#shouldRetry` or `#shouldRetryResponse` should be configured
   * @param shouldRetryResponse a PartialFunction over only the Try-wrapped returned [[Rep]]. Only one of
   *                            `#shouldRetry` or `#shouldRetryResponse` should be configured
   * @param start how long to delay before retrying
   * @param retries number of times to retry
   * @param retryBudget a [[com.twitter.finagle.service.RetryBudget]]. It is highly recommended to share a single
   *                    instance of [[com.twitter.finagle.service.RetryBudget]] between both retry and re-queue
   *                    filters to prevent retry storms. As such, use caution here when specifying a new retryBudget
   * @see [[https://twitter.github.io/finagle/guide/Clients.html#retries Finagle Client Retries]]
   * @see [[com.twitter.finagle.service.RetryFilter]]
   * @see [[com.twitter.finagle.service.RetryPolicy]]
   * @return [[ThriftClientFilterChain]]
   */
  def withConstantRetry(
    shouldRetry: PartialFunction[(Req, Try[Rep]), Boolean] = PartialFunction.empty,
    shouldRetryResponse: PartialFunction[Try[Rep], Boolean] = PartialFunction.empty,
    start: Duration,
    retries: Int,
    retryBudget: RetryBudget = budget.retryBudget
  ): ThriftClientFilterChain[Req, Rep] = {

    withRetryPolicy(
      constantRetryPolicy(
        delay = start * retryMultiplier,
        retries = retries,
        shouldRetry = chooseShouldRetryFunction(shouldRetry, shouldRetryResponse)
      )
    )
  }

  /**
   * Install a [[com.twitter.finagle.service.RetryFilter]] configured with a [[com.twitter.finagle.service.RetryPolicy]]
   * using backoffs that grow exponentially using [[com.twitter.finagle.service.Backoff#decorrelatedJittered]]. The jittered
   * maximum is the `start` duration * the `multiplier` value.
   *
   * @param shouldRetry a PartialFunction over the both the [[Req]] and a Try-wrapped returned [[Rep]]. Only one of
   *                    `#shouldRetry` or `#shouldRetryResponse` should be configured
   * @param shouldRetryResponse a PartialFunction over only the Try-wrapped returned [[Rep]]. Only one of
   *                            `#shouldRetry` or `#shouldRetryResponse` should be configured
   * @param start how long to delay before retrying
   * @param multiplier used to create a jitter with a random distribution between `start` and 3 times the previously selected value,
   *                   capped at `start` * `multiplier`. See: [[com.twitter.finagle.service.Backoff#decorrelatedJittered]]
   * @param retries number of times to retry
   *
   * @see [[com.twitter.finagle.service.RetryFilter]]
   * @see [[com.twitter.finagle.service.RetryPolicy]]
   * @return [[ThriftClientFilterChain]]
   */
  def withExponentialRetry(
    shouldRetry: PartialFunction[(Req, Try[Rep]), Boolean] = PartialFunction.empty,
    shouldRetryResponse: PartialFunction[Try[Rep], Boolean] = PartialFunction.empty,
    start: Duration,
    multiplier: Int,
    retries: Int
  ): ThriftClientFilterChain[Req, Rep] = {

    withRetryPolicy(
      exponentialRetryPolicy(
        start = start * retryMultiplier,
        multiplier = multiplier,
        numRetries = retries,
        shouldRetry = chooseShouldRetryFunction(shouldRetry, shouldRetryResponse)
      )
    )
  }

  /**
   * Install a [[com.twitter.finagle.service.RetryFilter]] configured with the given [[com.twitter.finagle.service.RetryPolicy]].
   *
   * @param retryPolicy the [[com.twitter.finagle.service.RetryPolicy]] to use
   * @param retryMsg a [[String]] message to display before retrying thr request.
   * @see [[com.twitter.finagle.service.RetryFilter]]
   * @see [[com.twitter.finagle.service.RetryPolicy]]
   * @return [[ThriftClientFilterChain]]
   */
  def withRetryPolicy(
    retryPolicy: RetryPolicy[(Req, Try[Rep])],
    retryMsg: ((Req, Try[Rep]), Duration) => String = defaultRetryMsg
  ): ThriftClientFilterChain[Req, Rep] = {

    retryFilter = new IncrementCounterFilter[Req, Rep](invocationsCounter)
      .andThen(
        new RetryFilter[Req, Rep](
          addRetryLogging(retryPolicy, retryMsg),
          retryTimer,
          scopedStatsReceiver,
          budget.retryBudget
        )
      )
    this
  }

  /**
   * Install a [[com.twitter.finagle.service.TimeoutFilter]] configuration with the given [[org.joda.time.Duration]] timeout.
   * This filter will be "above" and configured retry filter and thus includes retries.
   *
   * @param duration the [[org.joda.time.Duration]] timeout to apply to requests through the filter.
   * @see [[com.twitter.finagle.service.TimeoutFilter]]
   * @return [[ThriftClientFilterChain]]
   */
  def withTimeout(duration: Duration): ThriftClientFilterChain[Req, Rep] = {
    val twitterTimeout = duration * timeoutMultiplier

    timeoutFilter = new TimeoutFilter[Req, Rep](
      twitterTimeout,
      new GlobalRequestTimeoutException(twitterTimeout),
      DefaultTimer
    )
    this
  }

  /**
   * Install a [[com.twitter.finagle.service.TimeoutFilter]] configuration with the given tunable timeout.
   * This filter will be "above" any configured retry filter and thus includes retries.
   *
   * @param duration the Tunable[Duration] ([[org.joda.time.Duration]]) timeout to apply to requests through the filter.
   * @see [[com.twitter.finagle.service.TimeoutFilter]]
   * @return [[ThriftClientFilterChain]]
   */
  def withTimeout(duration: Tunable[Duration]): ThriftClientFilterChain[Req, Rep] = {
    val exceptionFn = (duration: Duration) =>
      new GlobalRequestTimeoutException(duration * timeoutMultiplier)

    timeoutFilter = new TimeoutFilter[Req, Rep](
      duration,
      exceptionFn,
      DefaultTimer
    )
    this
  }

  /**
   * Install a [[com.twitter.finagle.service.TimeoutFilter]] configuration with the given [[org.joda.time.Duration]] timeout.
   * This filter will always be "below" any configured retry filter and thus does NOT include retries.
   *
   * @param duration the [[org.joda.time.Duration]] timeout to apply to requests through the filter.
   * @see [[com.twitter.finagle.service.TimeoutFilter]]
   * @return [[ThriftClientFilterChain]]
   */
  def withRequestTimeout(duration: Duration): ThriftClientFilterChain[Req, Rep] = {
    val twitterTimeout = duration * timeoutMultiplier

    requestTimeoutFilter = new TimeoutFilter[Req, Rep](
      twitterTimeout,
      new IndividualRequestTimeoutException(twitterTimeout),
      DefaultTimer
    )
    this
  }

  /**
   * Install a [[com.twitter.finagle.service.TimeoutFilter]] configuration with the given tunable timeout.
   * This filter will always be "below" any configured retry filter and thus does NOT include retries.
   *
   * @param duration the Tunable[Duration] ([[org.joda.time.Duration]]) timeout to apply to requests through the filter.
   * @see [[com.twitter.finagle.service.TimeoutFilter]]
   * @return [[ThriftClientFilterChain]]
   */
  def withRequestTimeout(duration: Tunable[Duration]): ThriftClientFilterChain[Req, Rep] = {
    val exceptionFn = (duration: Duration) =>
      new IndividualRequestTimeoutException(duration * timeoutMultiplier)

    requestTimeoutFilter = new TimeoutFilter[Req, Rep](
      duration,
      exceptionFn,
      DefaultTimer
    )
    this
  }

  /**
   * Install a [[com.twitter.finagle.filter.RequestSemaphoreFilter]] using an [[com.twitter.concurrent.AsyncSemaphore]]
   * with an Optional `maxWaiters` as the limit on the number of waiters for permits.
   *
   * @param initialPermits must be positive
   * @param maxWaiters must be non-negative if set
   * @see [[com.twitter.finagle.filter.RequestSemaphoreFilter]]
   * @see [[com.twitter.concurrent.AsyncSemaphore]]
   * @return [[ThriftClientFilterChain]]
   */
  def withConcurrencyLimit(
    initialPermits: Int,
    maxWaiters: Option[Int] = None
  ): ThriftClientFilterChain[Req, Rep] = {
    concurrencyLimitFilter = maxWaiters match {
      case Some(waiters) =>
        new RequestSemaphoreFilter[Req, Rep](new AsyncSemaphore(initialPermits, waiters))
      case _ =>
        new RequestSemaphoreFilter[Req, Rep](new AsyncSemaphore(initialPermits))
    }
    this
  }

  /**
   * Install a [[com.twitter.inject.thrift.internal.filters.LatencyFilter]] that tracks the "logical" (overall) latency
   * distribution of a method invocation. This will INCLUDE any retries as it is installed at the top of the filter stack.
   *
   * @param statsReceiver a [[com.twitter.finagle.stats.StatsReceiver]] to track StatsFilter measurements. By default this
   *                      will be the class-level StatsReceiver scoped accordingly.
   * @param timeUnit this controls what granularity is used for measuring latency.  The default is milliseconds, but
   *                 other values are valid. The choice of this changes the name of the stat attached to the given
   *                 [[com.twitter.finagle.stats.StatsReceiver]]. For the common units, it will be "latency_ms".
   * @see [[com.twitter.finagle.service.ResponseClassifier]]
   * @see [[com.twitter.finagle.service.StatsFilter.DefaultExceptions]]
   * @return [[ThriftClientFilterChain]]
   */
  def withMethodLatency(
    statsReceiver: StatsReceiver = scopedStatsReceiver,
    timeUnit: TimeUnit = TimeUnit.MILLISECONDS
  ): ThriftClientFilterChain[Req, Rep] = {

    methodLatencyFilter =
      new LatencyFilter[Rep, Req](statsReceiver = scopedStatsReceiver, timeUnit = timeUnit)
        .asInstanceOf[Filter[Req, Rep, Req, Rep]]
    this
  }

  /** A parameter-less implementation with defaults */
  def withMethodLatency: ThriftClientFilterChain[Req, Rep] = {
    withMethodLatency(scopedStatsReceiver, TimeUnit.MILLISECONDS)
  }

  /**
   * Install a [[com.twitter.inject.thrift.internal.filters.LatencyFilter]] that tracks the per-request latency
   * distribution of a method invocation. This will per-retry if the invocation results in retried requests.
   *
   * @param statsReceiver a [[com.twitter.finagle.stats.StatsReceiver]] to track StatsFilter measurements. By default this
   *                      will be the class-level StatsReceiver scoped accordingly.
   * @param timeUnit this controls what granularity is used for measuring latency.  The default is milliseconds, but
   *                 other values are valid. The choice of this changes the name of the stat attached to the given
   *                 [[com.twitter.finagle.stats.StatsReceiver]]. For the common units, it will be "request_latency_ms".
   * @see [[com.twitter.finagle.service.ResponseClassifier]]
   * @see [[com.twitter.finagle.service.StatsFilter.DefaultExceptions]]
   * @return [[ThriftClientFilterChain]]
   */
  def withRequestLatency(
    statsReceiver: StatsReceiver = scopedStatsReceiver,
    timeUnit: TimeUnit = TimeUnit.MILLISECONDS
  ): ThriftClientFilterChain[Req, Rep] = {

    requestLatencyFilter = new LatencyFilter[Rep, Req](
      statsReceiver = scopedStatsReceiver,
      statName = "request_latency",
      timeUnit = timeUnit
    ).asInstanceOf[Filter[Req, Rep, Req, Rep]]
    this
  }

  /** A parameter-less implementation with defaults */
  def withRequestLatency: ThriftClientFilterChain[Req, Rep] = {
    withRequestLatency(scopedStatsReceiver, TimeUnit.MILLISECONDS)
  }

  /**
   * After this filter chain is executed this is the [[com.twitter.finagle.Service]] to invoke. The layer of indirection
   * is to allow for servers that wish to intercept the invocation of the bottom service.
   *
   * @param service the [[com.twitter.finagle.Service]] to invoke at the end of this filter chain
   * @return a [[com.twitter.finagle.Service]] with this filter chain applied
   */
  def andThen(service: Service[Req, Rep]): Service[Req, Rep] = {
    andThenService.andThen(method, this.toFilter, service)
  }

  /* Protected */

  protected def defaultRetryMsg(requestAndResponse: (Req, Try[Rep]), duration: Duration) = {
    val (_, response) = requestAndResponse
    s"Retrying ${ThriftMethodUtils.prettyStr(method)} = ${toDetailedExceptionMessage(response)} in ${duration.inMillis} ms"
  }

  /** Example scope: clnt/thrift/Adder/add1String */
  protected[thrift] def scopeStatsReceiver(): StatsReceiver = {
    statsReceiver.scope("clnt", clientLabel, method.serviceName, method.name)
  }

  /* Private */

  /**
   * @see scala.PartialFunction#applyOrElse
   */
  private val AlwaysFalse = Function.const(false) _

  /*
   * Note: If shouldRetryResponse is set, convert it into a partial function which accepts
   * both a request and a response. Since we are manually calling the partial function, we
   * call PartialFunction#applyOrElse to see if shouldRetryResponse matches the incoming
   * response else the result is a Function that always returns false (we are defensive and
   * return of false indicates we do not want to retry).
   */
  private def chooseShouldRetryFunction(
    shouldRetry: PartialFunction[(Req, Try[Rep]), Boolean],
    shouldRetryResponse: PartialFunction[Try[Rep], Boolean]
  ): PartialFunction[(Req, Try[Rep]), Boolean] = {
    assert(
      shouldRetryResponse != PartialFunction
        .empty[Try[Rep], Boolean] | shouldRetry != PartialFunction.empty[(Req, Try[Rep]), Boolean]
    )

    if (shouldRetry != PartialFunction.empty[(Req, Try[Rep]), Boolean]) {
      shouldRetry
    } else {
      case (request, responseTry) =>
        shouldRetryResponse.applyOrElse(responseTry, AlwaysFalse)
    }
  }

  private def addRetryLogging(
    retryPolicy: RetryPolicy[(Req, Try[Rep])],
    retryMsg: ((Req, Try[Rep]), Duration) => String
  ): RetryPolicy[(Req, Try[Rep])] = {

    new RetryPolicy[(Req, Try[Rep])] {
      override def apply(
        result: (Req, Try[Rep])
      ): Option[(twitter.util.Duration, RetryPolicy[(Req, Try[Rep])])] = {

        retryPolicy(result) match {
          case Some((duration, policy)) =>
            if (logger.isWarnEnabled) {
              val msg = retryMsg(result, duration)
              if (msg.nonEmpty) {
                warn(msg)
              }
            }
            Some((duration, addRetryLogging(policy, retryMsg)))
          case _ =>
            None
        }
      }
    }
  }

  private def exponentialRetryPolicy[T](
    start: Duration,
    multiplier: Int,
    numRetries: Int,
    shouldRetry: PartialFunction[T, Boolean]
  ): RetryPolicy[T] = {

    backoff(
      decorrelatedJittered(start, start * multiplier) take numRetries
    )(shouldRetry)
  }

  private def constantRetryPolicy[T](
    shouldRetry: PartialFunction[T, Boolean],
    delay: Duration,
    retries: Int
  ): RetryPolicy[T] = {

    backoff(constant(delay) take retries)(shouldRetry)
  }

  private[thrift] def toFilter: Filter[Req, Rep, Req, Rep] = {
    methodLatencyFilter
      .andThen(exceptionFilterImpl)
      .andThen(timeoutFilter)
      .andThen(retryFilter)
      .andThen(requestTimeoutFilter)
      .andThen(requestLatencyFilter)
      .andThen(concurrencyLimitFilter)
      .andThen(filterChain)
  }
}
