package com.twitter.finatra.kafkastreams.integration.async_transformer

import com.twitter.conversions.DurationOps._
import com.twitter.finatra.kafka.serde.ScalaSerdes
import com.twitter.finatra.kafkastreams.test.{FinatraTopologyTester, TopologyFeatureTest}
import org.apache.kafka.common.serialization.Serdes
import org.joda.time.DateTime

class WordLookupAsyncServerTopologyFeatureTest extends TopologyFeatureTest {

  override val topologyTester = FinatraTopologyTester(
    "async-server-prod-bob",
    new WordLookupAsyncServer,
    startingWallClockTime = new DateTime("2018-01-01T00:00:00Z")
  )

  private val textLinesTopic = topologyTester
    .topic("TextLinesTopic", ScalaSerdes.Long, Serdes.String)

  private val wordsWithCountsTopic = topologyTester
    .topic("WordToWordLength", Serdes.String, Serdes.Long)

  test("word count") {
    val messagePublishTime = topologyTester.now
    textLinesTopic.pipeInput(1L, "hello")

    // Trigger manual commit which is configured to run every 30 seconds
    topologyTester.advanceWallClockTime(30.seconds)

    val result = wordsWithCountsTopic.readOutput()
    result.key should equal("hello")
    result.value should equal(5)
    new DateTime(result.timestamp()) should equal(messagePublishTime)

    val latencyMetric = topologyTester.stats.getStat("kafka/stream/transform_async_latency_ms")
    latencyMetric.exists(_ >= 0) should be(true)
  }
}
