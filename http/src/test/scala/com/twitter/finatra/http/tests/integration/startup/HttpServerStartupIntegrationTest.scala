package com.twitter.finatra.http.tests.integration.startup

import com.twitter.finagle.http.Request
import com.twitter.finatra.http.routing.HttpRouter
import com.twitter.finatra.http.EmbeddedHttpServer
import com.twitter.finatra.http.{Controller, HttpServer}
import com.twitter.inject.Test
import com.twitter.util.Future

class HttpServerStartupIntegrationTest extends Test {

  test("Duplicate route paths fails server startup") {
    val server = new EmbeddedHttpServer(
      twitterServer = new HttpServer {
        override def configureHttp(router: HttpRouter): Unit = {
          router.add(new Controller {
            get("/foo") { _: Request => }
            get("/foo") { _: Request => }
          })
        }
      },
      disableTestLogging = true)

    try {
      val e = intercept[AssertionError] {
        server.start()
      }

      e.getMessage should be("assertion failed: Found non-unique routes GET     /foo")
    } finally {
      server.close()
    }
  }

  test("Empty callback which return String fails server startup") {
    val server = new EmbeddedHttpServer(
      twitterServer = new HttpServer {
        override def configureHttp(router: HttpRouter): Unit = {
          router.add(new Controller {
            get("/nothing") {
              "nothing"
            }
          })
        }
      },
      disableTestLogging = true)

    intercept[Exception] {
      server.start()
    }

    server.close()
  }

  test("Empty callback which returns a Response fails server startup") {
    val server = new EmbeddedHttpServer(
      twitterServer = new HttpServer {
        override def configureHttp(router: HttpRouter): Unit = {
          router.add(new Controller {
            options("/nothing") {
              response.ok
            }
          })
        }
      },
      disableTestLogging = true)

    intercept[Exception] {
      server.start()
    }

    server.close()
  }

  test("Empty callback which returns a Map[String, String] fails server startup") {
    val server = new EmbeddedHttpServer(
      twitterServer = new HttpServer {
        override def configureHttp(router: HttpRouter): Unit = {
          router.add(new Controller {
            options("/nothing") {
              Map("foo" -> "bar")
            }
          })
        }
      },
      disableTestLogging = true)

    intercept[Exception] {
      server.start()
    }

    server.close()
  }

  test("Empty callback which returns a Seq fails server startup") {
    val server = new EmbeddedHttpServer(
      twitterServer = new HttpServer {
        override def configureHttp(router: HttpRouter): Unit = {
          router.add(new Controller {
            get("/nothing") {
              Seq("1", "137")
            }
          })
        }
      },
      disableTestLogging = true)

    intercept[Exception] {
      server.start()
    }

    server.close()
  }

  test("Callback with parameter of type Int fails server startup") {
    val server = new EmbeddedHttpServer(
      twitterServer = new HttpServer {
        override def configureHttp(router: HttpRouter): Unit = {
          router.add(new Controller {
            get("/int") { _: Int =>
              "int"
            }
          })
        }
      },
      disableTestLogging = true)

    intercept[Exception] {
      server.start()
    }

    server.close()
  }

  test("Callback with input parameter of type String and response type of String fails server startup") {
    val server = new EmbeddedHttpServer(
      twitterServer = new HttpServer {
        override def configureHttp(router: HttpRouter): Unit = {
          router.add(new Controller {
            get("/int") { _: String =>
              "String"
            }
          })
        }
      },
      disableTestLogging = true)

    intercept[Exception] {
      server.start()
    }

    server.close()
  }

  test("Callback with input parameter of type String and response type of Response fails server startup") {
    val server = new EmbeddedHttpServer(
      twitterServer = new HttpServer {
        override def configureHttp(router: HttpRouter): Unit = {
          router.add(new Controller {
            get("/int") { _: String =>
              response.ok
            }
          })
        }
      },
      disableTestLogging = true)

    intercept[Exception] {
      server.start()
    }

    server.close()
  }
}
