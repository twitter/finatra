package com.twitter.finatra.streams.tests

import com.twitter.finatra.streams.converters.time._
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.Serde
import org.apache.kafka.streams.TopologyTestDriver
import org.apache.kafka.streams.test.ConsumerRecordFactory
import org.joda.time.{DateTime, DateTimeUtils}
import org.scalatest.Matchers

/**
 * Used to read/write from Kafka topics in the topology tester.
 *
 * @param topologyTestDriver the topology test driver
 * @param name the name of the topic
 * @param keySerde the serde for the key
 * @param valSerde the serde for the value
 * @tparam K the type of the key
 * @tparam V the type of the value
 */
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
    assert(outputRecord != null, "No output record is available for this assertion")

    if (key != outputRecord.key() || value != outputRecord.value()) {
      assert((outputRecord.key.toString -> outputRecord.value.toString) == (key -> value))
    }

    if (time != null && outputRecord.timestamp.toLong.iso8601Millis != time.getMillis.iso8601Millis) {
      assert(
        Tuple3(
          outputRecord.key(),
          outputRecord.value(),
          outputRecord.timestamp.toLong.iso8601Millis) ==
          Tuple3(key, value, time.getMillis.iso8601Millis))
    }
  }
}
