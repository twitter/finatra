package com.twitter

package object finatra {
  @deprecated("Use com.twitter.finatra.http.Controller", "")
  type Controller = com.twitter.finatra.http.Controller

  @deprecated("Use com.twitter.finatra.http.HttpServer", "")
  type HttpServer = com.twitter.finatra.http.HttpServer
}
