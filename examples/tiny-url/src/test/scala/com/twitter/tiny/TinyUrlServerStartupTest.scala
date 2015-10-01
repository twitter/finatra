package com.twitter.tiny

import com.google.inject.Stage
import com.twitter.finatra.http.test.EmbeddedHttpServer
import com.twitter.inject.server.FeatureTest

class TinyUrlServerStartupTest extends FeatureTest {

  override val server = new EmbeddedHttpServer(
    stage = Stage.PRODUCTION,
    twitterServer = new TinyUrlServer)

  "Server" should {
    "startup" in {
      server.assertAppStarted()
    }
  }
}
