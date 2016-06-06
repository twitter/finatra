package com.twitter.finatra.http.tests.integration.fileserver

import com.twitter.finagle.http.{Request, Status}
import com.twitter.finatra.http.filters.CommonFilters
import com.twitter.finatra.http.routing.HttpRouter
import com.twitter.finatra.http.EmbeddedHttpServer
import com.twitter.finatra.http.{Controller, HttpServer}
import com.twitter.inject.Test

class LocalFileServerFeatureTest extends Test {

  "local file mode" in {
    assertServer(
      new Controller {
        get("/foo") { request: Request =>
          response.ok.file("/abcd1234")
        }
      },
      flags = Map(
        "local.doc.root" -> "/tmp")) { server =>
      server.httpGet(
        "/foo",
        andExpect = Status.NotFound)
    }
  }

  "server file which is directory" in {
    assertServer(
      new Controller {
        get("/foo") { request: Request =>
          response.ok.file("/")
        }
      },
      flags = Map(
        "local.doc.root" -> "/asdfjkasdfjasdfj")) { server =>
      server.httpGet(
        "/foo",
        andExpect = Status.NotFound)
    }
  }

  private def assertServer(
    controller: Controller,
    flags: Map[String, String])(asserts: EmbeddedHttpServer => Unit) = {

    val server = new EmbeddedHttpServer(
      twitterServer = new HttpServer {
        override def configureHttp(router: HttpRouter) {
          router
            .filter[CommonFilters]
            .add(controller)
        }
      },
      flags = flags)

    try {
      asserts(server)
    }
    finally {
      server.close()
    }
  }
}
