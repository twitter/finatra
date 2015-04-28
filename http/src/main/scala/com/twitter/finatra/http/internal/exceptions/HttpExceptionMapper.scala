package com.twitter.finatra.http.internal.exceptions

import com.twitter.finagle.http.{Request, Response}
import com.twitter.finatra.http.exceptions._
import com.twitter.finatra.http.response.ResponseBuilder
import javax.inject.{Inject, Singleton}

@Singleton
class HttpExceptionMapper @Inject()(
  response: ResponseBuilder)
  extends ExceptionMapper[HttpException] {

  override def toResponse(request: Request, throwable: HttpException): Response =
    throwable.createResponse(response)
}
