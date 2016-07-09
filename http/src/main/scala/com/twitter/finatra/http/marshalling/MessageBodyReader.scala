package com.twitter.finatra.http.marshalling

import com.twitter.finagle.http.Request

trait MessageBodyReader[T] extends MessageBodyComponent {
  def parse[U: Manifest](request: Request): T
}
