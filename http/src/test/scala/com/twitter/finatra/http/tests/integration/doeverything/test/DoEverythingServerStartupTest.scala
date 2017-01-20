package com.twitter.finatra.http.tests.integration.doeverything.test

import com.google.inject.Stage
import com.twitter.finatra.http.EmbeddedHttpServer
import com.twitter.finatra.http.tests.integration.doeverything.main.DoEverythingServer
import com.twitter.inject.server.WordSpecFeatureTest

class DoEverythingServerStartupTest extends WordSpecFeatureTest {

  override val server = new EmbeddedHttpServer(
    twitterServer = new DoEverythingServer,
    stage = Stage.PRODUCTION)

  "DoEverythingServer" should {
    "startup" in {
      server.assertHealthy()
    }
  }
}
