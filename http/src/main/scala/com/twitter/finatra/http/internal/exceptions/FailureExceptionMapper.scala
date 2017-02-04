package com.twitter.finatra.http.internal.exceptions

import com.twitter.finagle.Failure
import com.twitter.finagle.http.{Request, Response}
import com.twitter.finatra.http.internal.exceptions.ThrowableExceptionMapper._
import com.twitter.finatra.http.response.ResponseBuilder
import com.twitter.inject.Logging
import javax.inject.{Inject, Singleton}

@Singleton
private[http] class FailureExceptionMapper @Inject()(
  response: ResponseBuilder)
  extends AbstractFrameworkExceptionMapper[Failure](response)
  with Logging {

  private val MaxDepth = 5

  override protected def handle(
    request: Request,
    response: ResponseBuilder,
    exception: Failure): Response = {
    unwrapFailure(exception, MaxDepth) match {
      case cause: Failure =>
        error("Unhandled Exception", exception)
        unhandledExceptionResponse(request, response, exception)
      case cause =>
        throw cause
    }
  }

  /* Private */

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