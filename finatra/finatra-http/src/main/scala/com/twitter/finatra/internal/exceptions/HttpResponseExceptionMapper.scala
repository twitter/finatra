package com.twitter.finatra.internal.exceptions

import com.twitter.finagle.http.{Request, Response}
import com.twitter.finatra.exceptions._
import javax.inject.{Inject, Singleton}

@Singleton
class HttpResponseExceptionMapper extends ExceptionMapper[HttpResponseException] {

  override def toResponse(request: Request, throwable: HttpResponseException): Response =
    throwable.response
}
