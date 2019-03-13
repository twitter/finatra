package com.twitter.finatra.http.tests.server

import com.twitter.finagle.Service
import com.twitter.finagle.http.{Request, Response, Status}
import com.twitter.finatra.http.{EmbeddedHttpServer, HttpServerTrait}
import com.twitter.inject.Test
import com.twitter.util.Future

class HttpServerTraitIntegrationTest extends Test {

  test("HiServiceServer#respond") {
    val server = new EmbeddedHttpServer(new HiServer)
    try {
      server.httpGet("/asdf", andExpect = Status.Ok, withBody = "hi")
    } finally {
      server.close()
    }
  }
}

class HiServer extends HttpServerTrait {

  /** Override with an implementation to serve an HTTP Service */
  override protected def httpService: Service[Request, Response] = new Service[Request, Response] {
    def apply(request: Request): Future[Response] = {
      val response = Response()
      response.statusCode(200)
      response.setContentString("hi")
      Future(response)
    }
  }
}
