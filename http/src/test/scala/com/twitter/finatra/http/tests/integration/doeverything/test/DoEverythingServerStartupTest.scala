package com.twitter.finatra.http.tests.integration.doeverything.test

import com.google.inject.Stage
import com.twitter.finatra.http.EmbeddedHttpServer
import com.twitter.finatra.http.tests.integration.doeverything.main.DoEverythingServer
import com.twitter.inject.server.FeatureTest

class DoEverythingServerStartupTest extends FeatureTest {

  override val server =
    new EmbeddedHttpServer(
      twitterServer = new DoEverythingServer,
      flags = Map("https.port" -> ":0"), // for testing `EmbeddedHttpServer.logStartup` method
      stage = Stage.PRODUCTION)

  test("DoEverythingServer#startup") {
    server.assertHealthy()
  }

  test("DoEverythingServer#support specifying GlobalFlags") {
    var shouldLogMetrics = false

    com.twitter.finagle.stats.logOnShutdown() should equal(false) // verify initial default value

    com.twitter.finagle.stats.logOnShutdown.let(false) { //set the scope of this test thread
      val server = new EmbeddedHttpServer(
        twitterServer = new DoEverythingServer {
          override protected def postInjectorStartup(): Unit = {
            // mutate to match the inner scope of withLocals
            shouldLogMetrics = com.twitter.finagle.stats.logOnShutdown()
            super.postInjectorStartup()
          }
        },
        globalFlags = Map(
          com.twitter.finagle.stats.logOnShutdown -> "true"
        )
      )
      try {
        server.start() // start the server, otherwise the scope will never be entered
        shouldLogMetrics should equal(true) // verify mutation of inner scope
        com.twitter.finagle.stats
          .logOnShutdown() should equal(false) // verify outer scope is not changed
      } finally {
        server.close()
      }
    }

    com.twitter.finagle.stats.logOnShutdown() should equal(false) // verify default value unchanged
  }
}
