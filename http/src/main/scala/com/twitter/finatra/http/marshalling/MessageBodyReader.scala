package com.twitter.finatra.http.marshalling

import com.twitter.finagle.http.Request

/**
 * [[MessageBodyReader]] transforms an HTTP Request into an object.
 */
trait MessageBodyReader[T] extends MessageBodyComponent {
  def parse[M <: T: Manifest](request: Request): T
}
