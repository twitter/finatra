package com.twitter.finatra.marshalling

import com.twitter.finatra.Request

trait MessageBodyReader[T] extends MessageBodyComponent {
  def parse(request: Request): T
}
