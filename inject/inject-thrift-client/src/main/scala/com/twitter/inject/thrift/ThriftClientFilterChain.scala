package com.twitter.inject.thrift

import com.twitter
import com.twitter.finagle._
import com.twitter.finagle.param.HighResTimer
import com.twitter.finagle.service.Backoff._
import com.twitter.finagle.service.RetryPolicy._
import com.twitter.finagle.service.{RetryFilter, RetryPolicy, TimeoutFilter}
import com.twitter.finagle.stats.StatsReceiver
import com.twitter.finagle.util.DefaultTimer
import com.twitter.inject.conversions.duration._
import com.twitter.inject.thrift.internal.IncrementCounterFilter
import com.twitter.inject.thrift.conversions.method._
import com.twitter.inject.thrift.internal.filters.ThriftClientExceptionFilter
import com.twitter.inject.utils.ExceptionUtils._
import com.twitter.inject.{Injector, Logging}
import com.twitter.scrooge.{ThriftMethod, ThriftResponse, ThriftStruct}
import com.twitter.util.{Throwables, Try}
import org.joda.time.Duration

class ThriftClientFilterChain[Req <: ThriftStruct, Rep <: ThriftResponse[_]](
  injector: Injector,
  statsReceiver: StatsReceiver,
  label: String,
  method: ThriftMethod)
  extends Logging {

  // clnt/adder-thrift/Adder/add1String
  private val methodStats = statsReceiver.scope("clnt").scope(label).scope(method.serviceName).scope(method.name)
  private val failuresCounter = methodStats.counter("failures")
  private val invocationsCounter = methodStats.counter("invocations")
  private val failuresScoped = methodStats.scope("failures")

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

  def constantRetry(
    requestTimeout: Duration,
    shouldRetry: PartialFunction[(Req, Try[Rep]), Boolean] = null,
    shouldRetryResponse: PartialFunction[Try[Rep], Boolean] = null,
    start: Duration,
    retries: Int) = {

    retry(
      constantRetryPolicy(
        delay = start,
        retries = retries,
        shouldRetry = chooseShouldRetryFunction(shouldRetry, shouldRetryResponse)))
      .requestTimeout(requestTimeout)
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
        start = start,
        multiplier = multiplier,
        numRetries = retries,
        shouldRetry = chooseShouldRetryFunction(shouldRetry, shouldRetryResponse)))
      .requestTimeout(requestTimeout)
  }

  def timeout(duration: Duration) = {
    val twitterTimeout = duration.toTwitterDuration

    filter(
      new TimeoutFilter[Req, Rep](
        twitterTimeout,
        new GlobalRequestTimeoutException(twitterTimeout),
        DefaultTimer.twitter))
  }

  def requestTimeout(duration: Duration) = {
    val twitterTimeout = duration.toTwitterDuration

    filter(
      new TimeoutFilter[Req, Rep](
        twitterTimeout,
        new IndividualRequestTimeoutException(twitterTimeout),
        DefaultTimer.twitter))
  }

  def retry(
    retryPolicy: RetryPolicy[(Req, Try[Rep])],
    retryMsg: ((Req, Try[Rep]), Duration) => String = defaultRetryMsg) = {

    filter(new IncrementCounterFilter[Req, Rep](invocationsCounter))

    filter(new RetryFilter[Req, Rep](
      addRetryLoggingAndStats(retryPolicy, retryMsg),
      HighResTimer.Default,
      methodStats))
  }

  def defaultRetryMsg(requestAndResponse: (Req, Try[Rep]), duration: Duration) = {
    val (_, response) = requestAndResponse
    val responseStr = stripNewlines(response.toString)
    "Retrying " + method.toPrettyString + s" = $responseStr in ${duration.getMillis} ms"
  }

  def andThen(service: Service[Req, Rep]): Service[Req, Rep] = {
    val exceptionFilterImpl = exceptionFilterOverride getOrElse new ThriftClientExceptionFilter[Req, Rep](method)
    exceptionFilterImpl andThen filterChain andThen service
  }

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
    shouldRetry:         PartialFunction[(Req, Try[Rep]), Boolean],
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

  private def addRetryLoggingAndStats(
    retryPolicy: RetryPolicy[(Req, Try[Rep])],
    retryMsg: ((Req, Try[Rep]), Duration) => String): RetryPolicy[(Req, Try[Rep])] = {

    new RetryPolicy[(Req, Try[Rep])] {
      override def apply(result: (Req, Try[Rep])): Option[(twitter.util.Duration, RetryPolicy[(Req, Try[Rep])])] = {
        incrRetryStats(result)

        retryPolicy(result) match {
          case Some((duration, policy)) =>

            val msg = retryMsg(result, duration.toJodaDuration)
            if (msg.nonEmpty) {
              warn(msg)
            }
            Some((duration, addRetryLoggingAndStats(policy, retryMsg)))
          case _ =>
            None
        }
      }
    }
  }

  private def incrRetryStats(result: (Req, Try[Rep])): Unit = {
    val (req, tryRep) = result
    tryRep.onFailure { e =>
      failuresCounter.incr()
      incrScopedFailureCounter(e)
    }
  }

  private def incrScopedFailureCounter(e: Throwable): Unit = {
    failuresScoped.counter(Throwables.mkString(e): _*).incr()
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
