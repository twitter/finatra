package com.twitter.finatra.http.marshalling

import com.twitter.finagle.http.Request

trait MessageBodyReader[T] extends MessageBodyComponent {
  def parse(request: Request): T
}
