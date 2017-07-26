package com.twitter.finatra.http.exceptions

import com.twitter.finagle.http.{Request, Response}

/** AbstractExceptionMapper for usage from Java */
abstract class AbstractExceptionMapper[T <: Throwable] extends ExceptionMapper[T]

/**
 * An ExceptionMapper converts a `T`-typed throwable to an HTTP response.
 *
 * Java users should use the [[AbstractExceptionMapper]].
 */
trait ExceptionMapper[T <: Throwable] {

  /**
   * Maps an exception of [[T]] to a [[com.twitter.finagle.http.Response]]
   * @param request - the incoming [[com.twitter.finagle.http.Request]]
   * @param throwable - the Exception [[T]] to handle
   * @return a valid [[com.twitter.finagle.http.Response]]
   */
  def toResponse(request: Request, throwable: T): Response
}
