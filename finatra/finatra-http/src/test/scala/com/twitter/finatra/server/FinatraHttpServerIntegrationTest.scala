package com.twitter.finatra.server

import com.twitter.finagle.Service
import com.twitter.finagle.http.{Request, Response, Status}
import com.twitter.finatra.test.{EmbeddedTwitterServer, Test}
import com.twitter.util.Future

class FinatraHttpServerIntegrationTest extends Test {

  "HiServiceServer" should {
    "respond" in {
      val server = EmbeddedTwitterServer(new HiServer)
      server.httpGet(
        "/asdf",
        andExpect = Status.Ok,
        withBody = "hi")
    }
  }
}

class HiServer extends FinatraHttpServer {
  override def httpService = new Service[Request, Response] {
    def apply(request: Request) = {
      val response = Response()
      response.setStatusCode(200)
      response.setContentString("hi")
      Future(response)
    }
  }
}
