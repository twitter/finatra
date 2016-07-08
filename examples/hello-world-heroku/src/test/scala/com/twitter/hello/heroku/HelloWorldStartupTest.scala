package com.twitter.hello.heroku

import com.codahale.metrics.MetricFilter
import com.google.inject.Stage
import com.twitter.finagle.metrics.MetricsStatsReceiver
import com.twitter.finatra.http.EmbeddedHttpServer
import com.twitter.inject.server.FeatureTest

class HelloWorldStartupTest extends FeatureTest {

  override val server = new EmbeddedHttpServer(
    twitterServer = new HelloWorldServer,
    stage = Stage.PRODUCTION,
    verbose = false)

  override def afterEach() {
    MetricsStatsReceiver.metrics.removeMatching(MetricFilter.ALL)
  }

  "Server" should {
    "startup" in {
      // Because we disabled the adminHttpServer we instead check the started flag.
      server.assertStarted()
    }
  }
}
