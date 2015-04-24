package com.twitter.hello

import com.twitter.finatra.HttpServer
import com.twitter.finatra.filters.CommonFilters
import com.twitter.finatra.routing.HttpRouter

object HelloWorldServerMain extends HelloWorldServer

class HelloWorldServer extends HttpServer {
  override def configureHttp(router: HttpRouter) {
    router.
      filter[CommonFilters].
      add[HelloWorldController]
  }
}
