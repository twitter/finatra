package com.twitter.finatra.multiserver.test

import com.twitter.adder.thriftscala.Adder
import com.twitter.finagle.http.Status
import com.twitter.finatra.http.EmbeddedHttpServer
import com.twitter.finatra.multiserver.Add1HttpServer.Add1Server
import com.twitter.inject.server.FeatureTest
import com.twitter.util.Future
import com.twitter.util.mock.Mockito

class Add1ServerFeatureTest extends FeatureTest with Mockito {

  val adderMpe = mock[Adder.MethodPerEndpoint]

  override val server =
    new EmbeddedHttpServer(new Add1Server)
      .bind[Adder.MethodPerEndpoint].toInstance(adderMpe)

  test("add1") {
    adderMpe.add1(5) returns Future(6)

    server.httpGet("/add1?num=5", andExpect = Status.Ok, withBody = "6")
  }
}
