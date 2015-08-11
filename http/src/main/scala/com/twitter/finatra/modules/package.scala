package com.twitter.finatra

package object modules {
  @deprecated("Use com.twitter.finatra.http.modules.AccessLogModule", "")
  val AccessLogModule = com.twitter.finatra.http.modules.AccessLogModule

  @deprecated("Use com.twitter.finatra.http.modules.MustacheModule", "")
  val MustacheModule = com.twitter.finatra.http.modules.MustacheModule
}
