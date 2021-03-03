package com.twitter.finatra.http.marshalling.modules

import com.twitter.finatra.http.marshalling.MessageBodyFlags
import com.twitter.inject.TwitterModule

object MessageBodyFlagsModule extends TwitterModule {
  // java-friendly access to singleton
  def get(): this.type = this

  flag(
    MessageBodyFlags.ResponseCharsetEnabled,
    true,
    "Return HTTP Response Content-Type UTF-8 Charset")
}
