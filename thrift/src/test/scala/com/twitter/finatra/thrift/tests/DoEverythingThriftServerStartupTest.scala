package com.twitter.finatra.thrift.tests

import com.google.inject.Stage
import com.twitter.finatra.thrift.EmbeddedThriftServer
import com.twitter.finatra.thrift.tests.doeverything.DoEverythingThriftServer
import com.twitter.inject.server.FeatureTest

class DoEverythingThriftServerStartupTest extends FeatureTest {

  override val server = new EmbeddedThriftServer(
    twitterServer = new DoEverythingThriftServer,
    stage = Stage.PRODUCTION)

  "Server" should {
    "start healthy" in {
      server.assertHealthy()
    }
  }
}
