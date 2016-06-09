package com.twitter.inject.thrift

import com.twitter
import com.twitter.concurrent.AsyncSemaphore
import com.twitter.finagle
import com.twitter.finagle._
import com.twitter.finagle.exp.BackupRequestFilter
import com.twitter.finagle.filter.RequestSemaphoreFilter
import com.twitter.finagle.param.HighResTimer
import com.twitter.finagle.service.Backoff._
import com.twitter.finagle.service.Retries.Budget
import com.twitter.finagle.service.RetryPolicy._
import com.twitter.finagle.service.{RetryBudget, RetryFilter, RetryPolicy, TimeoutFilter}
import com.twitter.finagle.stats.StatsReceiver
import com.twitter.finagle.util.{DefaultTimer, HashedWheelTimer}
import com.twitter.inject.conversions.duration._
import com.twitter.inject.thrift.internal.IncrementCounterFilter
import com.twitter.inject.thrift.internal.filters.ThriftClientExceptionFilter
import com.twitter.inject.thrift.utils.ThriftMethodUtils
import com.twitter.inject.utils.ExceptionUtils._
import com.twitter.inject.{Injector, Logging}
import com.twitter.scrooge.{ThriftMethod, ThriftResponse, ThriftStruct}
import com.twitter.util.{Duration => TwitterDuration, Throwables, Timer, Try}
import org.joda.time.Duration

class ThriftClientFilterChain[Req <: ThriftStruct, Rep <: ThriftResponse[_]](
  injector: Injector,
  statsReceiver: StatsReceiver,
  clientLabel: String,
  budget: Budget,
  method: ThriftMethod,
  timeoutMultiplier: Int,
  retryMultiplier: Int,
  useHighResTimerForRetries: Boolean,
  andThenService: AndThenService)
  extends Logging {

  private val retryTimer = {
    if(useHighResTimerForRetries)
      HighResTimer.Default
    else
      finagle.param.Timer.param.default.timer
  }

  // clnt/thrift/Adder/add1String
  private val methodStats = statsReceiver.scope("clnt").scope(clientLabel).scope(method.serviceName).scope(method.name)
  private val failuresCounter = methodStats.counter("failures")
  // method invocations - incremented every time we call/invoke the method.
  private val invocationsCounter = methodStats.counter("invocations")
  private val failuresScope = methodStats.scope("failures")

  // Mutable
  private var filterChain: Filter[Req, Rep, Req, Rep] = Filter.identity
  private var exceptionFilterOverride: Option[Filter[Req, Rep, Req, Rep]] = None

  /* Public */

  def filter(filter: Filter[Req, Rep, Req, Rep]): ThriftClientFilterChain[Req, Rep] = {
    filterChain = filterChain andThen filter
    this
  }

  def globalFilter(filter: Filter[ThriftStruct, ThriftResponse[_], ThriftStruct, ThriftResponse[_]]): ThriftClientFilterChain[Req, Rep] = {
    filterChain = filterChain andThen filter.asInstanceOf[Filter[Req, Rep, Req, Rep]]
    this
  }

  def filter[T <: Filter[Req, Rep, Req, Rep] : Manifest]: ThriftClientFilterChain[Req, Rep] = {
    filter(injector.instance[T])
  }

  def globalFilter[T <: Filter[ThriftStruct, ThriftResponse[_], ThriftStruct, ThriftResponse[_]] : Manifest]: ThriftClientFilterChain[Req, Rep] = {
    globalFilter(injector.instance[T])
  }

  def exceptionFilter[T <: Filter[Req, Rep, Req, Rep] : Manifest]: ThriftClientFilterChain[Req, Rep] = {
    exceptionFilter(injector.instance[T])
  }

  def exceptionFilter(filter: Filter[Req, Rep, Req, Rep]): ThriftClientFilterChain[Req, Rep] = {
    exceptionFilterOverride = Some(filter)
    this
  }

  /*
   * Note: It’s highly recommended to share a single instance of RetryBudget between
   * both RetryFilter and RequeueFilter to prevent retry storms. As such, use caution
   * when specifying the retryBudget. See https://twitter.github.io/finagle/guide/Clients.html#retries
   */
  def constantRetry(
    requestTimeout: Duration,
    shouldRetry: PartialFunction[(Req, Try[Rep]), Boolean] = null,
    shouldRetryResponse: PartialFunction[Try[Rep], Boolean] = null,
    start: Duration,
    retries: Int,
    retryBudget: RetryBudget = budget.retryBudget) = {

    retry(
      constantRetryPolicy(
        delay = start.multipliedBy(retryMultiplier),
        retries = retries,
        shouldRetry = chooseShouldRetryFunction(shouldRetry, shouldRetryResponse)))
      .requestTimeout(requestTimeout)
  }

  def backupRequestFilter(
    quantile: Int,
    clipDuration: Duration,
    history: Duration,
    timer: Timer = HashedWheelTimer.Default) = {
    filter(
      new BackupRequestFilter[Req, Rep](
        quantile = quantile,
        clipDuration = clipDuration.toTwitterDuration * timeoutMultiplier,
        timer = timer,
        statsReceiver = methodStats,
        history = history.toTwitterDuration))
  }

  def exponentialRetry(
    requestTimeout: Duration,
    shouldRetry: PartialFunction[(Req, Try[Rep]), Boolean] = null,
    shouldRetryResponse: PartialFunction[Try[Rep], Boolean] = null,
    start: Duration,
    multiplier: Int,
    retries: Int): ThriftClientFilterChain[Req, Rep] = {

    retry(
      exponentialRetryPolicy(
        start = start.multipliedBy(retryMultiplier),
        multiplier = multiplier,
        numRetries = retries,
        shouldRetry = chooseShouldRetryFunction(shouldRetry, shouldRetryResponse)))
      .requestTimeout(requestTimeout)
  }

  def timeout(duration: Duration) = {
    val twitterTimeout = duration.toTwitterDuration * timeoutMultiplier

    filter(
      new TimeoutFilter[Req, Rep](
        twitterTimeout,
        new GlobalRequestTimeoutException(twitterTimeout),
        DefaultTimer.twitter))
  }

  def requestTimeout(duration: Duration) = {
    val twitterTimeout = duration.toTwitterDuration * timeoutMultiplier

    filter(
      new TimeoutFilter[Req, Rep](
        twitterTimeout,
        new IndividualRequestTimeoutException(twitterTimeout),
        DefaultTimer.twitter))
  }

  def retry(
    retryPolicy: RetryPolicy[(Req, Try[Rep])],
    retryMsg: ((Req, Try[Rep]), TwitterDuration) => String = defaultRetryMsg) = {

    filter(new IncrementCounterFilter[Req, Rep](invocationsCounter))

    filter(new RetryFilter[Req, Rep](
      addRetryLogging(retryPolicy, retryMsg),
      retryTimer,
      methodStats,
      budget.retryBudget))
  }

  def concurrencyLimit(initialPermits: Int, maxWaiters: Int) = {
    filter(new RequestSemaphoreFilter[Req, Rep](new AsyncSemaphore(initialPermits, maxWaiters)))
  }

  def defaultRetryMsg(requestAndResponse: (Req, Try[Rep]), duration: TwitterDuration) = {
    val (_, response) = requestAndResponse
    s"Retrying ${ThriftMethodUtils.prettyStr(method)} = ${toDetailedExceptionMessage(response)} in ${duration.inMillis} ms"
  }

  def andThen(service: Service[Req, Rep]): Service[Req, Rep] = {
    val exceptionFilterImpl = exceptionFilterOverride getOrElse new ThriftClientExceptionFilter[Req, Rep](clientLabel, method)
    val filterChainToAdd = exceptionFilterImpl andThen filterChain
    andThenService.andThen(method, filterChainToAdd, service)
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
    assert(shouldRetryResponse != null | shouldRetry != null)

    if (shouldRetry != null) {
      shouldRetry
    }
    else {
      case (request, responseTry) =>
        shouldRetryResponse.applyOrElse(responseTry, AlwaysFalse)
    }
  }

  private def addRetryLogging(
    retryPolicy: RetryPolicy[(Req, Try[Rep])],
    retryMsg: ((Req, Try[Rep]), TwitterDuration) => String): RetryPolicy[(Req, Try[Rep])] = {

    new RetryPolicy[(Req, Try[Rep])] {
      override def apply(result: (Req, Try[Rep])): Option[(twitter.util.Duration, RetryPolicy[(Req, Try[Rep])])] = {
        incrRetryStats(result)

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

  private def incrRetryStats(result: (Req, Try[Rep])): Unit = {
    val (_, rep) = result
    rep.onFailure { e =>
      failuresCounter.incr()
      incrScopedFailureCounter(e)
    }
  }

  private def incrScopedFailureCounter(e: Throwable): Unit = {
    failuresScope.counter(Throwables.mkString(e): _*).incr()
  }

  private def exponentialRetryPolicy[T](
    start: Duration,
    multiplier: Int,
    numRetries: Int,
    shouldRetry: PartialFunction[T, Boolean]): RetryPolicy[T] = {

    backoff(
      exponential(start.toTwitterDuration, multiplier) take numRetries)(shouldRetry)
  }

  private def constantRetryPolicy[T](
    shouldRetry: PartialFunction[T, Boolean],
    delay: Duration,
    retries: Int): RetryPolicy[T] = {

    backoff(
      constant(delay.toTwitterDuration) take retries)(shouldRetry)
  }
}
