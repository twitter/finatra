package com.twitter.hello

import com.twitter.finagle.http.Status._
import com.twitter.finatra.http.test.EmbeddedHttpServer
import com.twitter.inject.server.FeatureTest

class HelloWorldFeatureTest extends FeatureTest {

  override val server = new EmbeddedHttpServer(new HelloWorldServer)

  "Server" should {
    "Say hi" in {
      server.httpGet(
        path = "/hi?name=Bob",
        andExpect = Ok,
        withBody = "Hello Bob")
    }
    "Say hi for Post" in {
      server.httpPost(
        path = "/hi",
        postBody =
          """
          {
            "id": 10,
            "name" : "Sally"
          }
          """,
        andExpect = Ok,
        withBody = "Hello Sally with id 10")
    }
  }
}
