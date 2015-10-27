package com.twitter.finatra.http.internal.exceptions

import com.google.common.net.MediaType
import com.twitter.finagle.http.{Request, Response}
import com.twitter.finagle.{CancelledRequestException, Failure}
import com.twitter.finatra.exceptions.ExternalServiceExceptionMatcher
import com.twitter.finatra.http.exceptions.{DefaultExceptionMapper, HttpException, HttpResponseException}
import com.twitter.finatra.http.internal.exceptions.FinatraDefaultExceptionMapper._
import com.twitter.finatra.http.response.{ErrorsResponse, ResponseBuilder}
import com.twitter.inject.Logging
import javax.inject.{Inject, Singleton}

object FinatraDefaultExceptionMapper {
  private val MaxDepth = 5

  private def unwrapFailure(failure: Failure, depth: Int): Throwable = {
    if (depth == 0)
      failure
    else
      failure.cause match {
        case Some(inner: Failure) => unwrapFailure(inner, depth - 1)
        case Some(cause) => cause
        case None => failure
      }
  }
}

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
      case e: Failure =>
        unwrapFailure(e, MaxDepth) match {
          case cause: Failure =>
            error("internal server error", e)
            response.internalServerError.json(
              ErrorsResponse(request, e, "internal server error"))
          case cause =>
            toResponse(request, cause)
        }
      case e =>
        // ExceptionMappingFilter protects us against fatal exceptions.
        error("internal server error", e)
        response.internalServerError.json(
          ErrorsResponse(request, e, "internal server error"))
    }
  }
}
