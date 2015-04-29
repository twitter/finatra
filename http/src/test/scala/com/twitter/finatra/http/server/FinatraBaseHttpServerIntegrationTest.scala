package com.twitter.finatra.http.server

import com.twitter.finagle.Service
import com.twitter.finagle.http.{Request, Response, Status}
import com.twitter.finatra.http.internal.server.BaseHttpServer
import com.twitter.finatra.http.test.EmbeddedHttpServer
import com.twitter.inject.Test
import com.twitter.util.Future

class FinatraBaseHttpServerIntegrationTest extends Test {

  "HiServiceServer" should {
    "respond" in {
      val server = new EmbeddedHttpServer(new HiServer)
      server.httpGet(
        "/asdf",
        andExpect = Status.Ok,
        withBody = "hi")
    }
  }
}

class HiServer extends BaseHttpServer {
  override def httpService = new Service[Request, Response] {
    def apply(request: Request) = {
      val response = Response()
      response.setStatusCode(200)
      response.setContentString("hi")
      Future(response)
    }
  }
}
