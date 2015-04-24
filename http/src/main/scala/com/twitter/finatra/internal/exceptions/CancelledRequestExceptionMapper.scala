package com.twitter.finatra.internal.exceptions

import com.twitter.finagle.CancelledRequestException
import com.twitter.finagle.http.{Request, Response}
import com.twitter.finatra.exceptions._
import com.twitter.finatra.response.ResponseBuilder
import javax.inject.{Inject, Singleton}

@Singleton
class CancelledRequestExceptionMapper @Inject()(
  response: ResponseBuilder)
  extends ExceptionMapper[CancelledRequestException] {

  override def toResponse(request: Request, throwable: CancelledRequestException): Response =
    response.clientClosed
}
