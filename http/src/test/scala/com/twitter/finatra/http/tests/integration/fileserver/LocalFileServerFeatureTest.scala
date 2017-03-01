package com.twitter.finatra.http.tests.integration.fileserver

import com.twitter.finagle.http.{Request, Status}
import com.twitter.finatra.http.filters.CommonFilters
import com.twitter.finatra.http.routing.HttpRouter
import com.twitter.finatra.http.EmbeddedHttpServer
import com.twitter.finatra.http.{Controller, HttpServer}
import com.twitter.finatra.test.LocalFilesystemTestUtils._
import com.twitter.inject.Test
import org.apache.commons.io.FileUtils

class LocalFileServerFeatureTest extends Test {

  test("local file mode") {
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

  test("server file which is directory") {
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

  test("server existing file") {
    val path = "/tmp"
    val filename = "finatra-test-file.txt"
    val fileContent = "file content"
    FileUtils.writeStringToFile(createFile(s"$path/$filename"), fileContent)
    assertServer(
      new Controller {
        get("/foo") { request: Request =>
          response.ok.fileOrIndex(filename, "index.html")
        }
      },
      flags = Map(
        "local.doc.root" -> path)) { server =>
      server.httpGet(
        "/foo",
        andExpect = Status.Ok,
        withBody = fileContent)
    }
  }

  test("server index when file doesn't exists") {
    val path = "/tmp"
    val indexName = "index.html"
    val filename = "non-existing-file.txt"
    val indexContent = "index content"
    FileUtils.writeStringToFile(createFile(s"$path/$indexName"), indexContent)
    assertServer(
      new Controller {
        get("/foo") { request: Request =>
          response.ok.fileOrIndex(filename, indexName)
        }
      },
      flags = Map(
        "local.doc.root" -> path)) { server =>
      server.httpGet(
        "/foo",
        andExpect = Status.Ok,
        withBody = indexContent)
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
