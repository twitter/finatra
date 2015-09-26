package com.twitter.finatra.http.integration.startup

import com.twitter.finagle.httpx.Request
import com.twitter.finatra.http.routing.HttpRouter
import com.twitter.finatra.http.test.EmbeddedHttpServer
import com.twitter.finatra.http.{Controller, HttpServer}
import com.twitter.inject.Test

class HttpServerStartupIntegrationTest extends Test {

  "admin endpoints must be /admin/finatra" in {
    val server = new EmbeddedHttpServer(
      twitterServer = new HttpServer {
        override def configureHttp(router: HttpRouter): Unit = {
          router.add(new Controller {
            get("/admin/foo") { request: Request =>
            }
          })
        }
      })

    intercept[AssertionError] {
      server.start()
    }

    server.close()
  }

  "Duplicate route paths fails server startup" in {
    val server = new EmbeddedHttpServer(
      twitterServer = new HttpServer {
        override def configureHttp(router: HttpRouter): Unit = {
          router.add(new Controller {
            get("/foo") { request: Request =>
            }
            get("/foo") { request: Request =>
            }
          })
        }
      })

    val e = intercept[AssertionError] {
      server.start()
    }

    server.close()
    e.getMessage should be("assertion failed: Found non-unique routes GET     /foo")
  }
}
