package com.twitter.finatra.kafkastreams.test

import org.apache.kafka.streams.TopologyTestDriver
import org.apache.kafka.streams.processor.StateStore
import org.apache.kafka.streams.processor.internals.RecordCollector
import org.apache.kafka.test.MockRecordCollector

/**
 * Factory to allow us to call the package private RocksDBStore constructor
 */
object KafkaTestUtil {
  def createNoopRecord(): RecordCollector = {
    new MockRecordCollector
  }

  def getStore(driver: TopologyTestDriver, name: String): StateStore = {
    driver.getStateStore(name)
  }
}
