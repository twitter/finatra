package com.twitter.finatra.logback.integration.server

import com.twitter.finagle.http.Request
import com.twitter.finatra.http.{Controller, HttpServer}
import com.twitter.finatra.http.routing.HttpRouter
import com.twitter.inject.Logging

object AppendTestHttpServerMain extends AppendTestHttpServer

class AppendTestHttpServer extends HttpServer with Logging {
  override protected def configureHttp(router: HttpRouter): Unit = {
    router.add(new Controller {
      get("/log_events") { request: Request =>
        warn("Logging initial warning")

        for (x <- 1 to 5) {
          info(s"Logging looped info - $x")

          trace(s"Logging looped trace - $x")

          debug(s"Logging looped debug - $x")

          warn(s"Logging looped warning - $x")

          error(s"Logging looped error - $x")
        }
        response.ok("pong")
      }
    })
  }
}
