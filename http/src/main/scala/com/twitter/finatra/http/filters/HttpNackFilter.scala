package com.twitter.finatra.http.filters

import com.twitter.finagle.http.filter.{HttpNackFilter => Nacks}
import com.twitter.finagle.http.{Request, Response}
import com.twitter.finagle.stats.StatsReceiver
import com.twitter.finagle.{Service, SimpleFilter}
import com.twitter.finatra.http.filters.HttpNackFilter._
import com.twitter.finatra.http.exceptions.HttpNackException
import com.twitter.util.Future
import javax.inject.{Inject, Singleton}

private object HttpNackFilter {
  val RetryableHttpNackException: Future[Response] =
    Future.exception(new HttpNackException(retryable = true))
  val NonRetryableHttpNackException: Future[Response] =
    Future.exception(new HttpNackException(retryable = false))
}

/**
 * This Filter converts Finagle nacks into [[HttpNackException]]s to be handled by
 * a registered `ExceptionMapper[HttpNackException]`. A "nack" is a Negative ACKnowledgement.
 * Responding with a nack is one way a service may exert back pressure.
 *
 * @see [[com.twitter.finatra.http.internal.exceptions.HttpNackExceptionMapper]]
 * @see [[com.twitter.finagle.http.filter.HttpNackFilter Finagle HttpNackFilter]]
 * @see [[https://twitter.github.io/finagle/guide/Glossary.html?highlight=nack Finagle NACK]]
 */
@Singleton
class HttpNackFilter[R <: Request] @Inject()(statsReceiver: StatsReceiver)
    extends SimpleFilter[R, Response] {

  def apply(request: R, service: Service[R, Response]): Future[Response] = {
    service(request).rescue {
      case Nacks.RetryableNack() =>
        RetryableHttpNackException
      case Nacks.NonRetryableNack() =>
        NonRetryableHttpNackException
    }
  }
}
