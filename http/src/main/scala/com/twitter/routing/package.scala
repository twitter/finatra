package com.twitter.finatra

package object routing {
  @deprecated("Use com.twitter.finatra.http.routing.HttpRouter", "")
  type Router = com.twitter.finatra.http.routing.HttpRouter

  @deprecated("Use com.twitter.finatra.http.routing.FileResolver", "")
  type FileResolver = com.twitter.finatra.http.routing.FileResolver

  @deprecated("Use com.twitter.finatra.http.routing.HttpRouter", "")
  type HttpRouter = com.twitter.finatra.http.routing.HttpRouter

  @deprecated("Use com.twitter.finatra.http.routing.HttpRouter", "")
  val HttpRouter = com.twitter.finatra.http.routing.HttpRouter
}
