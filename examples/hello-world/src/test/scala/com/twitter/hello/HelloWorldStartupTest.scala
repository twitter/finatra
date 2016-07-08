package com.twitter.hello

import com.google.inject.Stage
import com.twitter.finatra.http.EmbeddedHttpServer
import com.twitter.inject.server.FeatureTest

class HelloWorldStartupTest extends FeatureTest {

  override val server = new EmbeddedHttpServer(
    twitterServer = new HelloWorldServer,
    stage = Stage.PRODUCTION,
    verbose = false)

  "Server" should {
    "startup" in {
      server.assertHealthy()
    }
  }
}
