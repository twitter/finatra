package com.twitter.finatra.http.integration.multiserver.test

import com.twitter.finagle.httpx.Status
import com.twitter.finatra.http.integration.multiserver.add1server.Add1Server
import com.twitter.finatra.http.test.{EmbeddedHttpServer, HttpTest}

class Add1ServerFeatureTest extends HttpTest {

  val add1Server = new EmbeddedHttpServer(new Add1Server)

  "add1" in {
    add1Server.httpGet(
      "/add1?num=5",
      andExpect = Status.Ok,
      withBody = "6")
  }
}
