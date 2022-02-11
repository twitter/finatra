package com.twitter.finatra.http.modules

import com.twitter.finagle.http.Request
import com.twitter.finagle.http.Response
import com.twitter.finatra.http.exceptions.ExceptionMapper
import com.twitter.finatra.http.exceptions.HttpResponseException
import com.twitter.finatra.http.response.ResponseBuilder
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HttpResponseExceptionMapper @Inject() (response: ResponseBuilder)
    extends ExceptionMapper[HttpResponseException] {

  override def toResponse(request: Request, e: HttpResponseException): Response = {
    response.create(e.response)
  }
}
