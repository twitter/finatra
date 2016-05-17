package com.twitter.finatra.http.tests.integration.multiserver.add2server

import com.twitter.finatra.http.HttpServer
import com.twitter.finatra.http.routing.HttpRouter

class Add2Server extends HttpServer {
  override val modules = Seq(Add1HttpClientModule)

  override def configureHttp(router: HttpRouter) {
    router.add[Add2Controller]
  }
}
