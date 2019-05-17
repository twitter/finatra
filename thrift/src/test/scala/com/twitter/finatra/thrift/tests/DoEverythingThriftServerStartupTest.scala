package com.twitter.finatra.thrift.tests

import com.google.inject.Stage
import com.twitter.finatra.thrift.EmbeddedThriftServer
import com.twitter.finatra.thrift.tests.doeverything.DoEverythingThriftServer
import com.twitter.inject.server.FeatureTest

class DoEverythingThriftServerStartupTest extends FeatureTest {

  override val server =
    new EmbeddedThriftServer(
      twitterServer = new DoEverythingThriftServer,
      disableTestLogging = true,
      stage = Stage.PRODUCTION)

  test("Server start healthy") {
    server.assertHealthy()
  }

  test("support specifying GlobalFlags") {
    var shouldLogMetrics = false

    com.twitter.finagle.stats.logOnShutdown() should equal(false) // verify initial default value

    com.twitter.finagle.stats.logOnShutdown.let(false) { // set the scope of this test thread
      val server = new EmbeddedThriftServer(
        twitterServer = new DoEverythingThriftServer {
          override protected def postInjectorStartup(): Unit = {
            // mutate to match the inner scope of withLocals
            shouldLogMetrics = com.twitter.finagle.stats.logOnShutdown()
            super.postInjectorStartup()
          }
        },
        disableTestLogging = true,
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
