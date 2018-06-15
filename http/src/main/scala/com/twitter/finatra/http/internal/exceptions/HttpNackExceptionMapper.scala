package com.twitter.finatra.http.internal.exceptions

import com.twitter.finagle.http.filter.HttpNackFilter
import com.twitter.finagle.http.{Request, Response, Status}
import com.twitter.finatra.http.exceptions.HttpNackException
import com.twitter.finatra.http.response.ResponseBuilder
import javax.inject.{Inject, Singleton}

/**
 * Default [[com.twitter.finatra.http.exceptions.ExceptionMapper]] responsible for converting
 * [[HttpNackException]] types into an HTTP Response.
 */
@Singleton
private[http] class HttpNackExceptionMapper @Inject()(response: ResponseBuilder)
  extends AbstractFrameworkExceptionMapper[HttpNackException](response) {

  override protected def handle(
    request: Request,
    response: ResponseBuilder,
    exception: HttpNackException
  ): Response = {
    val header: (String, String) = if (exception.retryable) {
      (HttpNackFilter.RetryableNackHeader, "true")
    } else {
      (HttpNackFilter.NonRetryableNackHeader, "true")
    }

    response
      .status(Status.ServiceUnavailable)
      .headers(header)
  }
}
