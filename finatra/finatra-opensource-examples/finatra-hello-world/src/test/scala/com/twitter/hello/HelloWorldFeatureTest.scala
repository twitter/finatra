package com.twitter.hello

import com.twitter.finagle.http.Status._
import com.twitter.finatra.test.{EmbeddedTwitterServer, HttpTest}

class HelloWorldFeatureTest extends HttpTest {

  val server = EmbeddedTwitterServer(new HelloWorldServer)

  "Server" should {
    "Say hi" in {
      server.httpGet(
        path = "/hi?name=Bob",
        andExpect = Ok,
        withBody = "Hello Bob")
    }
  }
}
