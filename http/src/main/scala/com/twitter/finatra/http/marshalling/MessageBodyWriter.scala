package com.twitter.finatra.http.marshalling

import com.twitter.finagle.http.Request

/**
 * MessageBodyWriter's transform objects into HTTP Responses
 */
trait MessageBodyWriter[T] extends MessageBodyComponent {
  def write(request: Request, obj: T): WriterResponse = write(obj)
  def write(obj: T): WriterResponse
}
