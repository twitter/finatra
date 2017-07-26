package com.twitter.finatra.http.internal.exceptions

import com.twitter.finagle.http.{Request, Response}
import com.twitter.finatra.http.exceptions.ExceptionMapper
import com.twitter.finatra.http.response.ResponseBuilder

private[exceptions] abstract class AbstractFrameworkExceptionMapper[T <: Throwable](
  response: ResponseBuilder
) extends ExceptionMapper[T] {

  /**
   * Maps an exception of [[T]] to a [[com.twitter.finagle.http.Response]]
   *
   * @param request   - the incoming [[com.twitter.finagle.http.Request]]
   * @param throwable - the Exception [[T]] to handle
   * @return a valid [[com.twitter.finagle.http.Response]]
   */
  final override def toResponse(request: Request, throwable: T): Response = {
    handle(request, response, throwable)
  }

  protected def handle(request: Request, response: ResponseBuilder, exception: T): Response
}
