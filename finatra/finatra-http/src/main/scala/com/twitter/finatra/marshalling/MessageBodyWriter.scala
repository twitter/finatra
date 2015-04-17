package com.twitter.finatra.marshalling

/**
 * MessageBodyWriter's transform objects into HTTP Responses
 */
trait MessageBodyWriter[T] extends MessageBodyComponent {

  def write(obj: T): WriterResponse
}
