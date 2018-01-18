package com.twitter.finatra.http.marshalling

import com.twitter.finagle.http.Request

/**
 * [[MessageBodyWriter]] transforms an object into an HTTP Response.
 */
trait MessageBodyWriter[T] extends MessageBodyComponent {
  def write(request: Request, obj: T): WriterResponse = write(obj)
  def write(obj: T): WriterResponse
}
