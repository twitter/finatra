package com.twitter.finatra.marshalling

import com.twitter.finatra.Request

trait DefaultMessageBodyReader {
  def parse[T: Manifest](request: Request): T
}
