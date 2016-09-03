package com.twitter.tiny

import com.google.inject.Stage
import com.twitter.finatra.http.EmbeddedHttpServer
import com.twitter.inject.server.FeatureTest

class TinyUrlServerStartupTest extends FeatureTest {

  override val server = new EmbeddedHttpServer(
    twitterServer = new TinyUrlServer,
    stage = Stage.PRODUCTION)

  "Server" should {
    "startup" in {
      // Because we disabled the adminHttpServer we instead check the started flag.
      server.assertStarted()
    }
  }
}
