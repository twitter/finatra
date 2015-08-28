package com.twitter.finatra.http.integration.multiserver.test

import com.twitter.finagle.httpx.Status
import com.fasterxml.jackson.databind.JsonNode
import com.twitter.finatra.http.integration.multiserver.add1server.Add1Server
import com.twitter.finatra.http.integration.multiserver.add2server.Add2Server
import com.twitter.finatra.http.integration.tweetexample.main.TweetsEndpointServer
import com.twitter.finatra.http.test.{HttpTest, EmbeddedHttpServer}
import com.twitter.finatra.httpclient.RequestBuilder
import com.twitter.inject.server.FeatureTest
import com.twitter.util.{Await, Future, FuturePool}

class MultiServerFeatureTest extends HttpTest {

  val add1Server = new EmbeddedHttpServer(
    new Add1Server)

  val add2Server = new EmbeddedHttpServer(
    new Add2Server,
    clientFlags = Map(
      resolverMap("add1-server", add1Server)))

  "add2" in {
    add2Server.httpGet(
      "/add2?num=5",
      andExpect = Status.Ok,
      withBody = "7")
  }
}
