package com.twitter.finatra.streams.tests

import com.twitter.finatra.streams.converters.time._
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.Serde
import org.apache.kafka.streams.TopologyTestDriver
import org.apache.kafka.streams.test.ConsumerRecordFactory
import org.joda.time.{DateTime, DateTimeUtils}
import org.scalatest.Matchers

class TopologyTesterTopic[K, V](
  topologyTestDriver: => TopologyTestDriver,
  name: String,
  keySerde: Serde[K],
  valSerde: Serde[V])
    extends Matchers {

  private val recordFactory =
    new ConsumerRecordFactory(name, keySerde.serializer, valSerde.serializer)

  def pipeInput(key: K, value: V, timestamp: Long = DateTimeUtils.currentTimeMillis()): Unit = {
    topologyTestDriver.pipeInput(recordFactory.create(key, value, timestamp))
  }

  def readOutput(): ProducerRecord[K, V] = {
    topologyTestDriver.readOutput(name, keySerde.deserializer(), valSerde.deserializer())
  }

  def readAllOutput(): Seq[ProducerRecord[K, V]] = {
    Iterator
      .continually(readOutput())
      .takeWhile(_ != null)
      .toSeq
  }

  def assertOutput(key: K, value: V, time: DateTime = null): Unit = {
    val outputRecord = readOutput()
    assert(outputRecord.key() == key)
    assert(outputRecord.value() == value)
    if (time != null) {
      assert(outputRecord.timestamp.toLong.iso8601Millis == time.getMillis.iso8601Millis)
    }
  }
}
