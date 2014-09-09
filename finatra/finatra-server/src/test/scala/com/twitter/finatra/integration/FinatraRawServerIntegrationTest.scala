package com.twitter.finatra.integration

import com.twitter.finagle.Service
import com.twitter.finagle.http.{Request, Response, Status}
import com.twitter.finatra.FinatraRawServer
import com.twitter.finatra.test.{EmbeddedTwitterServer, Test}
import com.twitter.util.Future

class FinatraRawServerIntegrationTest extends Test {

  "HiServiceServer" should {
    "respond" in {
      val server = EmbeddedTwitterServer(HiServer)
      server.httpGet(
        "/asdf",
        andExpect = Status.Ok,
        withBody = "hi")
    }
  }
}

object HiServer extends FinatraRawServer {
  override def httpService = new Service[Request, Response] {
    def apply(request: Request) = {
      val response = Response()
      response.setStatusCode(200)
      response.setContentString("hi")
      Future(response)
    }
  }
}
