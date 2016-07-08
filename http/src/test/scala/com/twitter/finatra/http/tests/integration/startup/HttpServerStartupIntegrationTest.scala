package com.twitter.finatra.http.tests.integration.startup

import com.twitter.finagle.http.Request
import com.twitter.finatra.http.routing.HttpRouter
import com.twitter.finatra.http.EmbeddedHttpServer
import com.twitter.finatra.http.{Controller, HttpServer}
import com.twitter.inject.Test

class HttpServerStartupIntegrationTest extends Test {

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

    try {
      val e = intercept[AssertionError] {
        server.start()
      }

      e.getMessage should be("assertion failed: Found non-unique routes GET     /foo")
    }
    finally {
      server.close()
    }
  }

  "Empty callbacks fails server startup" in {
    val server = new EmbeddedHttpServer(
      twitterServer = new HttpServer {
        override def configureHttp(router: HttpRouter): Unit = {
          router.add(new Controller {
            get("/nothing") {
              "nothing"
            }
          })
        }
      })

    intercept[Exception] {
      server.start()
    }

    server.close()
  }

  "Callback with parameter of type Int fails server startup" in {
    val server = new EmbeddedHttpServer(
      twitterServer = new HttpServer {
        override def configureHttp(router: HttpRouter): Unit = {
          router.add(new Controller {
            get("/int") { r: Int =>
              "int"
            }
          })
        }
      })

    intercept[Exception] {
      server.start()
    }

    server.close()
  }
}
