package com.twitter.finatra.http.internal.exceptions

import com.twitter.finagle.CancelledRequestException
import com.twitter.finagle.http.{Request, Response}
import com.twitter.finatra.http.internal.exceptions.ThrowableExceptionMapper._
import com.twitter.finatra.http.response.ResponseBuilder
import com.twitter.finatra.utils.DeadlineValues
import javax.inject.{Inject, Singleton}

@Singleton
private[http] class CancelledRequestExceptionMapper @Inject()(
  response: ResponseBuilder)
  extends AbstractExceptionMapper[CancelledRequestException](response) {

  private val CancelledRequestExceptionDetails = Seq("CancelledRequestException")

  override protected def handle(
    request: Request,
    response: ResponseBuilder,
    exception: CancelledRequestException): Response = {
    val deadlineValues = DeadlineValues.current()

    response
      .clientClosed
      .failureClassifier(
        deadlineValues.exists(_.expired),
        request,
        source = DefaultExceptionSource,
        details = CancelledRequestExceptionDetails,
        message = deadlineValues.map(_.elapsed).getOrElse("unknown") + " ms")
  }
}