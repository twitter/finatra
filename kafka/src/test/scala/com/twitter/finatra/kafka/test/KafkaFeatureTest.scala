package com.twitter.finatra.kafka.test

import com.twitter.finatra.kafka.test.utils.InMemoryStatsUtil
import com.twitter.inject.Test
import com.twitter.inject.server.EmbeddedTwitterServer

trait KafkaFeatureTest extends Test with EmbeddedKafka {

  protected def server: EmbeddedTwitterServer

  override def beforeAll(): Unit = {
    super.beforeAll()
    server.start()
  }

  // Note: We close the server connected to kafka before closing the embedded kafka server
  override def afterAll(): Unit = {
    try {
      server.close()
    } finally {
      super.afterAll()
    }
  }

  protected lazy val inMemoryStatsUtil = InMemoryStatsUtil(server.injector)

  protected def waitForKafkaMetric(name: String, expected: Float): Unit = {
    inMemoryStatsUtil.waitForGauge(name, expected)
  }
}
