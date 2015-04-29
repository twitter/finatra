package com.twitter.finatra.http.internal.exceptions

import com.twitter.finagle.http.{Request, Response}
import com.twitter.finatra.http.exceptions._
import javax.inject.{Inject, Singleton}

@Singleton
class HttpResponseExceptionMapper extends ExceptionMapper[HttpResponseException] {

  override def toResponse(request: Request, throwable: HttpResponseException): Response =
    throwable.response
}
