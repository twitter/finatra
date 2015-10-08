package com.twitter.finatra.http.marshalling

import com.twitter.finagle.http.Request

trait DefaultMessageBodyReader {
  def parse[T: Manifest](request: Request): T
}
