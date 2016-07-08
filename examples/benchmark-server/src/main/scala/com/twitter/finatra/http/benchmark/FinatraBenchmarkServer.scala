package com.twitter.finatra.http.benchmark

import com.twitter.finagle.http.Request
import com.twitter.finatra.http.routing.HttpRouter
import com.twitter.finatra.http.{Controller, HttpServer}

object FinatraBenchmarkServerMain extends FinatraBenchmarkServer

class FinatraBenchmarkServer extends HttpServer {
  override def configureHttp(router: HttpRouter) {
    router
      .add(
        new Controller {
          get("/json") { request: Request =>
            Map("message" -> "Hello, World!")
          }

          get("/json/:id") { request: Request =>
            Map("message" -> ("Hello " + request.params("id")))
          }

          get("/hi") { request: Request =>
            "Hello " + request.params.getOrElse("name", "unnamed")
          }
        })
  }
}
