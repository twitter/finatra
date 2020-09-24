package com.twitter.finatra.kafkastreams.test

import org.apache.kafka.streams.TopologyTestDriver
import org.apache.kafka.streams.processor.StateStore
import org.apache.kafka.test.NoOpRecordCollector
import org.apache.kafka.streams.processor.internals.RecordCollector

/**
 * Factory to allow us to call the package private RocksDBStore constructor
 */
object KafkaTestUtil {
  def createNoopRecord(): RecordCollector = {
    new NoOpRecordCollector
  }

  def getStore(driver: TopologyTestDriver, name: String): StateStore = {
    driver.getStateStore(name)
  }

}
