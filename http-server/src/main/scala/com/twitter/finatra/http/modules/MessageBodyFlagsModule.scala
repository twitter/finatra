package com.twitter.finatra.http.modules

import com.twitter.finatra.http.marshalling.MessageBodyFlags
import com.twitter.inject.TwitterModule

object MessageBodyFlagsModule extends TwitterModule {

  flag(
    MessageBodyFlags.ResponseCharsetEnabled,
    true,
    "Return HTTP Response Content-Type UTF-8 Charset")
}
