package com.twitter.finatra.http.modules

import com.twitter.finagle.filter.LogFormatter
import com.twitter.finagle.http.filter.CommonLogFormatter
import com.twitter.finagle.http.{Request, Response}
import com.twitter.inject.TwitterModule
import javax.inject.Singleton

/**
 * A [[com.twitter.inject.TwitterModule]] which provides a
 * [[com.twitter.finagle.filter.LogFormatter]] implementation.
 */
object AccessLogModule extends TwitterModule {

  override def configure(): Unit = {
    bind[LogFormatter[Request, Response]].to[CommonLogFormatter].in[Singleton]
  }
}
