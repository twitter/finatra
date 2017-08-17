package com.twitter.web.dashboard

import com.google.inject.Stage
import com.twitter.finatra.http.EmbeddedHttpServer
import com.twitter.inject.server.FeatureTest

class ServerStartupTest extends FeatureTest {

  override val server =
    new EmbeddedHttpServer(twitterServer = new Server, stage = Stage.PRODUCTION, verbose = false)

  test("Server#startup") {
    server.assertHealthy()
  }
}
