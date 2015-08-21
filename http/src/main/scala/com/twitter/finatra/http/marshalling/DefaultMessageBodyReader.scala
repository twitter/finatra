package com.twitter.finatra.http.marshalling

import com.twitter.finagle.httpx.Request

trait DefaultMessageBodyReader {
  def parse[T: Manifest](request: Request): T
}
