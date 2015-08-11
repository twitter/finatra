package com.twitter.finatra.http.internal.exceptions

import com.google.common.net.MediaType
import com.twitter.finagle.CancelledRequestException
import com.twitter.finagle.http.{Request, Response}
import com.twitter.finatra.exceptions.ExternalServiceExceptionMatcher
import com.twitter.finatra.http.exceptions.{DefaultExceptionMapper, HttpException, HttpResponseException}
import com.twitter.finatra.http.response.{ResponseBuilder, ErrorsResponse}
import com.twitter.inject.Logging
import javax.inject.{Inject, Singleton}

@Singleton
class FinatraDefaultExceptionMapper @Inject()(
  response: ResponseBuilder)
  extends DefaultExceptionMapper
  with Logging {

  override def toResponse(request: Request, throwable: Throwable): Response = {
    throwable match {
      case e: HttpException =>
        val builder = response.status(e.statusCode)
        if (e.mediaType.is(MediaType.JSON_UTF_8))
          builder.json(ErrorsResponse(e.errors))
        else
          builder.plain(e.errors.mkString(", "))
      case e: HttpResponseException =>
        e.response
      case e: CancelledRequestException =>
        response.clientClosed
      case ExternalServiceExceptionMatcher(e) =>
        warn("service unavailable", e)
        response.serviceUnavailable.json(
          ErrorsResponse(request, e, "service unavailable"))
      case e =>
        // ExceptionMappingFilter protects us against fatal exceptions.
        error("internal server error", e)
        response.internalServerError.json(
          ErrorsResponse(request, e, "internal server error"))
    }
  }
}
