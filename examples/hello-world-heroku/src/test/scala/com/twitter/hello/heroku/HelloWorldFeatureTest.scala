package com.twitter.hello.heroku

import com.codahale.metrics.MetricFilter
import com.twitter.finagle.http.Status._
import com.twitter.finagle.metrics.MetricsStatsReceiver
import com.twitter.finatra.http.EmbeddedHttpServer
import com.twitter.inject.server.FeatureTest

class HelloWorldFeatureTest extends FeatureTest {

  override val server = new EmbeddedHttpServer(new HelloWorldServer)

  override def afterEach() {
    MetricsStatsReceiver.metrics.removeMatching(MetricFilter.ALL)
  }

  // TODO: turn back on after finagle-metrics upgrades to the newer version of util-stats
  ignore("Server#Say hi") {
    server.httpGet(
      path = "/hi?name=Bob",
      andExpect = Ok,
      withBody = "Hello Bob")
  }
}
