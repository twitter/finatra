package com.twitter.finatra.http.internal.exceptions

import com.google.common.net.MediaType
import com.twitter.finagle.http.{Request, Response}
import com.twitter.finagle.{CancelledRequestException, Failure}
import com.twitter.finatra.http.exceptions.{DefaultExceptionMapper, HttpException, HttpResponseException}
import com.twitter.finatra.http.internal.exceptions.FinatraDefaultExceptionMapper._
import com.twitter.finatra.http.response.{ErrorsResponse, ResponseBuilder}
import com.twitter.finatra.utils.DeadlineValues
import com.twitter.inject.Logging
import com.twitter.inject.utils.ExceptionUtils._
import javax.inject.{Inject, Singleton}
import org.apache.thrift.TException

private[http] object FinatraDefaultExceptionMapper {
  private val MaxDepth = 5
  private val DefaultExceptionSource = "Internal"

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
private[http] class FinatraDefaultExceptionMapper @Inject()(
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
        response.create(e.response)
      case e: CancelledRequestException =>
        val deadlineValues = DeadlineValues.current()
        response
          .clientClosed
          .failureClassifier(
            deadlineValues.exists(_.expired),
            request,
            source = DefaultExceptionSource,
            details = Seq("CancelledRequestException"),
            message = deadlineValues.map(_.elapsed).getOrElse("unknown") + " ms")
      case e: Failure =>
        unwrapFailure(e, MaxDepth) match {
          case cause: Failure =>
            unhandledResponse(request, e)
          case cause =>
            toResponse(request, cause)
        }
      case e: TException =>
        unhandledResponse(request, e, logStackTrace = false)
      case e => // Note: We don't use NonFatal(e) since ExceptionMappingFilter is protecting us
        unhandledResponse(request, e)
    }
  }

  private def unhandledResponse(request: Request, e: Throwable, logStackTrace: Boolean = true): Response = {
    if (logStackTrace) {
      error("Unhandled Exception", e)
    }
    else {
      error("Unhandled Exception: " + e)
    }

    response
      .internalServerError
      .failure(
        request,
        source = DefaultExceptionSource,
        details = Seq(
          "Unhandled",
          toExceptionDetails(e)))
      .jsonError
  }
}
