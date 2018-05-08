package com.twitter.finatra.sample

import com.google.inject.Stage
import com.twitter.inject.server.{EmbeddedTwitterServer, FeatureTest}

class ExampleTwitterServerStartupTest extends FeatureTest {

  val server = new EmbeddedTwitterServer(
    twitterServer = new ExampleTwitterServer,
    stage = Stage.PRODUCTION
  )

  test("Server#startup") {
    server.assertHealthy()
  }
}
