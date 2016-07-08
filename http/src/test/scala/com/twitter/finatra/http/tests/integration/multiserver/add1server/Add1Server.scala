package com.twitter.finatra.http.tests.integration.multiserver.add1server

import com.twitter.finatra.http.HttpServer
import com.twitter.finatra.http.routing.HttpRouter

class Add1Server extends HttpServer {
  override def configureHttp(router: HttpRouter) {
    router
      .add[Add1Controller]
  }
}
