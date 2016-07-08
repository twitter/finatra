package com.twitter.finatra.http.modules

import com.twitter.finagle.http.{Request, Response}
import com.twitter.finatra.http.exceptions.{ExceptionMapper, HttpResponseException}
import com.twitter.finatra.http.response.ResponseBuilder
import com.twitter.inject.Logging
import javax.inject.{Inject, Singleton}

@Singleton
class HttpResponseExceptionMapper @Inject()(
  response: ResponseBuilder)
  extends ExceptionMapper[HttpResponseException]
  with Logging {

  override def toResponse(request: Request, e: HttpResponseException): Response = {
    response.create(e.response)
  }
}
