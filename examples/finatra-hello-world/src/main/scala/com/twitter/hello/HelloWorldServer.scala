package com.twitter.hello

import com.twitter.finatra.http.HttpServer
import com.twitter.finatra.http.filters.CommonFilters
import com.twitter.finatra.http.routing.HttpRouter

object HelloWorldServerMain extends HelloWorldServer

class HelloWorldServer extends HttpServer {
  override def configureHttp(router: HttpRouter) {
    router.
      filter[CommonFilters].
      add[HelloWorldController]
  }
}
