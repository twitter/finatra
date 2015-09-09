package com.twitter.tiny.modules

import com.twitter.inject.TwitterModule

object TinyUrlModule extends TwitterModule {

  flag("secure", false, "Use HTTPS shortened URLS")
}
