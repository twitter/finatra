package com.twitter.finatra.http.integration.startup

import com.twitter.finagle.httpx.Request
import com.twitter.finatra.http.routing.HttpRouter
import com.twitter.finatra.http.{Controller, HttpServer}
import com.twitter.inject.Test
import com.twitter.inject.server.EmbeddedTwitterServer

class HttpServerStartupIntegrationTest extends Test {

  "admin endpoints must be /admin/finatra" in {
    intercept[AssertionError] {
      new EmbeddedTwitterServer(
        twitterServer = new HttpServer {
          override def configureHttp(router: HttpRouter): Unit = {
            router.add(new Controller {
              get("/admin/foo") { request: Request =>
              }
            })
          }
        }).start()
    }
  }

}
