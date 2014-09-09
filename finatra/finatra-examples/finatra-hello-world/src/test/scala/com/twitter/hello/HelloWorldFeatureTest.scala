package com.twitter.hello

import com.twitter.finagle.http.Status._
import com.twitter.finatra.test.{HttpTest, EmbeddedTwitterServer, HttpFeatureTest}

class HelloWorldFeatureTest extends HttpTest {

  val server = EmbeddedTwitterServer(HelloWorldServerMain)

  "HelloWorld Server" should {
    "answer yo" in {
      server.httpGet(
        path = "/hi",
        andExpect = Ok,
        withBody = "yo")
    }
  }
}
