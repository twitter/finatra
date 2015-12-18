package com.twitter.calculator

import com.google.inject.Stage
import com.twitter.finatra.thrift.EmbeddedThriftServer
import com.twitter.inject.server.FeatureTest

class CalculatorServerStartupTest extends FeatureTest {

  val server = new EmbeddedThriftServer(
    twitterServer = new CalculatorServer,
    stage = Stage.PRODUCTION)

  "server" should {
    "startup" in {
      server.assertHealthy()
    }
  }
}