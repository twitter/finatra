package com.twitter.finatra.http.integration.multiserver.test

import com.twitter.finagle.http.Status
import com.twitter.finatra.http.integration.multiserver.add1server.Add1Server
import com.twitter.finatra.http.test.EmbeddedHttpServer
import com.twitter.inject.server.FeatureTest

class Add1ServerFeatureTest extends FeatureTest {

  override val server = new EmbeddedHttpServer(new Add1Server)

  "add1" in {
    server.httpGet(
      "/add1?num=5",
      andExpect = Status.Ok,
      withBody = "6")
  }
}
