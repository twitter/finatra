package com.twitter.finatra.streams.tests

import com.twitter.inject.Test

abstract class TopologyFeatureTest extends Test {

  protected def topologyTester: FinatraTopologyTester

  override def beforeEach(): Unit = {
    super.beforeEach()
    topologyTester.reset()
  }

  override def afterAll(): Unit = {
    super.afterAll()
    topologyTester.close()
  }
}
