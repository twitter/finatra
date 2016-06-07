package com.twitter.finatra.multiserver.test

import com.google.inject.testing.fieldbinder.Bind
import com.twitter.adder.thriftscala.Adder
import com.twitter.finagle.http.Status
import com.twitter.finatra.http.EmbeddedHttpServer
import com.twitter.finatra.multiserver.Add1HttpServer.Add1Server
import com.twitter.inject.Mockito
import com.twitter.inject.server.FeatureTest
import com.twitter.util.Future

class Add1ServerFeatureTest extends FeatureTest with Mockito {

  @Bind
  val adderFuture = mock[Adder[Future]]

  override val server = new EmbeddedHttpServer(new Add1Server)

  "add1" in {
    adderFuture.add1(5) returns Future(6)

    server.httpGet(
      "/add1?num=5",
      andExpect = Status.Ok,
      withBody = "6")
  }
}
