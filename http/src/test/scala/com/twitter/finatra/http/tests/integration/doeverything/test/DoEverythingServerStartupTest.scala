package com.twitter.finatra.http.tests.integration.doeverything.test

import com.google.inject.Stage
import com.twitter.finatra.http.EmbeddedHttpServer
import com.twitter.finatra.http.tests.integration.doeverything.main.DoEverythingServer
import com.twitter.inject.server.FeatureTest

class DoEverythingServerStartupTest extends FeatureTest {

  override val server =
    new EmbeddedHttpServer(
      twitterServer = new DoEverythingServer,
      flags = Map(
        "something.flag" -> "foobar",
        "https.port" -> ":0"
      ), // for testing `EmbeddedHttpServer.logStartup` method
      stage = Stage.PRODUCTION)

  test("DoEverythingServer#startup") {
    server.assertHealthy()
  }
}
