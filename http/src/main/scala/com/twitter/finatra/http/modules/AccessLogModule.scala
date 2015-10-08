package com.twitter.finatra.http.modules

import com.twitter.finagle.filter.LogFormatter
import com.twitter.finagle.http.filter.CommonLogFormatter
import com.twitter.finagle.http.{Request, Response}
import com.twitter.inject.TwitterModule

object AccessLogModule extends TwitterModule {

  override def configure() {
    bindSingleton[LogFormatter[Request, Response]].to[CommonLogFormatter]
  }
}
