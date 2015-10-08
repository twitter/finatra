package com.twitter.finatra.http.exceptions

import com.twitter.finagle.http.{Request, Response}

/**
 * An ExceptionMapper converts a `T`-typed throwable to an HTTP response.
 */
trait ExceptionMapper[T <: Throwable] {
  def toResponse(request: Request, throwable: T): Response
}
