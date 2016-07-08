package com.twitter.finatra.multiserver.test

import com.twitter.finagle.http.{Request, Status}
import com.twitter.finatra.http.{EmbeddedHttpServer, HttpMockResponses}
import com.twitter.finatra.httpclient.HttpClient
import com.twitter.finatra.multiserver.Add2HttpServer.Add2Server
import com.twitter.inject.Mockito
import com.twitter.inject.server.FeatureTest
import com.twitter.util.Future

class Add2ServerFeatureTest extends FeatureTest with Mockito with HttpMockResponses {

  val mockHttpClient = smartMock[HttpClient]

  override val server =
    new EmbeddedHttpServer(new Add2Server)
        .bind[HttpClient](mockHttpClient)

  "add2" in {
    mockHttpClient.execute(any[Request]) returns(
      Future(ok("6")),
      Future(ok("7")))

    server.httpGet(
      "/add2?num=5",
      andExpect = Status.Ok,
      withBody = "7")
  }
}
