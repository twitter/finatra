package com.twitter.finatra.http.internal.exceptions

import com.twitter.finagle.CancelledRequestException
import com.twitter.finagle.http.{Request, Response}
import com.twitter.finatra.exceptions.ExternalServiceExceptionMatcher
import com.twitter.finatra.http.exceptions.DefaultExceptionMapper
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
