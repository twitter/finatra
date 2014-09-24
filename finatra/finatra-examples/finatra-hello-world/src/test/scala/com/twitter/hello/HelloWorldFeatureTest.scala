package com.twitter.hello

import com.twitter.finagle.http.Status._
import com.twitter.finatra.test.{HttpTest, EmbeddedTwitterServer, HttpFeatureTest}

class HelloWorldFeatureTest extends HttpTest {

  val server = EmbeddedTwitterServer(new HelloWorldServer)

  "HelloWorld Server" should {
    "ping" in {
      server.httpGet(
        path = "/ping",
        andExpect = Ok,
        withBody = "Hello World!")
    }
  }
}
