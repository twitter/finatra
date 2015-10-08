package com.twitter.finatra.http.integration.multiserver.test

import com.google.inject.testing.fieldbinder.Bind
import com.twitter.finagle.http.{Request, Status}
import com.twitter.finatra.http.integration.multiserver.add2server.Add2Server
import com.twitter.finatra.http.test.{HttpMockResponses, EmbeddedHttpServer}
import com.twitter.finatra.httpclient.HttpClient
import com.twitter.inject.Mockito
import com.twitter.inject.server.FeatureTest
import com.twitter.util.Future

class Add2ServerFeatureTest extends FeatureTest with Mockito with HttpMockResponses {

  @Bind
  val httpClient: HttpClient = smartMock[HttpClient]

  override val server = new EmbeddedHttpServer(new Add2Server)

  "add2" in {
    httpClient.execute(any[Request]) returns(
      Future(ok("6")),
      Future(ok("7")))

    server.httpGet(
      "/add2?num=5",
      andExpect = Status.Ok,
      withBody = "7")
  }
}
