package com.twitter.tiny.exceptions

import java.net.MalformedURLException
import javax.inject.Inject

import com.twitter.finagle.http.{Response, Request}
import com.twitter.finatra.http.exceptions.ExceptionMapper
import com.twitter.finatra.http.response.ResponseBuilder

class MalformedURLExceptionMapper @Inject()(response: ResponseBuilder)
  extends ExceptionMapper[MalformedURLException] {

  override def toResponse(request: Request, exception: MalformedURLException): Response = {
    response.badRequest(s"Malformed URL - ${exception.getMessage}")
  }
}
