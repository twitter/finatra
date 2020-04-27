package com.twitter.finatra.example

import com.google.inject.Stage
import com.twitter.inject.server.{EmbeddedTwitterServer, FeatureTest}

class ExampleTwitterServerStartupTest extends FeatureTest {

  val server: EmbeddedTwitterServer = new EmbeddedTwitterServer(
    twitterServer = new ExampleTwitterServer,
    disableTestLogging = true,
    stage = Stage.PRODUCTION
  )

  test("Server#startup") {
    server.assertHealthy()
  }
}
