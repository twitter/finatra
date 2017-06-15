package com.twitter.finatra.http.filters

import com.twitter.finagle.http.filter.{HttpNackFilter => FinagleHttpNackFilter}
import com.twitter.finagle.http.{Request, Response}
import com.twitter.finagle.stats.StatsReceiver
import com.twitter.finagle.{Service, SimpleFilter}
import com.twitter.finatra.http.exceptions.HttpNackException
import com.twitter.util.Future
import javax.inject.{Inject, Singleton}

/**
 * Finatra's exception handling/mapping conflicts with Finagle's internal
 * signaling mechanisms. This filter converts Finagle nack exceptions into
 * Finatra's HttpExceptions which can be handled appropriately by the
 * ExceptionMapper
 */
@Singleton
class HttpNackFilter[R <: Request] @Inject()(
    statsReceiver: StatsReceiver)
  extends SimpleFilter[R, Response] {
  import FinagleHttpNackFilter._

  private[this] val RetryableHttpNackException =
    Future.exception(new HttpNackException(retryable = true))
  private[this] val NonRetryableHttpNackException =
    Future.exception(new HttpNackException(retryable = false))

  def apply(req: R, service: Service[R, Response]): Future[Response] = {
    service(req).rescue {
      case RetryableNack(_) => RetryableHttpNackException
      case NonRetryableNack(_) => NonRetryableHttpNackException
    }
  }
}