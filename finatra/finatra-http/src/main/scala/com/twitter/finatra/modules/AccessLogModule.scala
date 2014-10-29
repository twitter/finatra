package com.twitter.finatra.modules

import com.twitter.finagle.filter.LogFormatter
import com.twitter.finagle.http.filter.CommonLogFormatter
import com.twitter.finagle.http.{Request, Response}
import com.twitter.finatra.guice.GuiceModule

object AccessLogModule extends GuiceModule {

  override def configure() {
    bindSingleton[LogFormatter[Request, Response]].to[CommonLogFormatter]
  }
}
