package com.twitter.finatra.kafkastreams.integration.record_headers

import com.twitter.finatra.kafkastreams.test.{FinatraTopologyTester, TopologyFeatureTest}
import org.apache.kafka.common.header.internals.{RecordHeader, RecordHeaders}
import org.apache.kafka.common.serialization.Serdes
import org.joda.time.DateTime

class RecordHeadersServerTopologyFeatureTest extends TopologyFeatureTest {
  private[this] val MatchingHeaderValue: Int = 0
  private[this] val NonMatchingHeaderValue: Int = 1

  override val topologyTester = FinatraTopologyTester(
    kafkaApplicationId = "recordheaders-prod",
    server = new RecordHeadersServer(filterValue = MatchingHeaderValue),
    startingWallClockTime = new DateTime("2018-01-01T00:00:00Z")
  )

  private val recordHeadersTopic =
    topologyTester.topic("RecordHeadersTopic", Serdes.String, Serdes.String)

  private val recordHeadersOutputTopic =
    topologyTester.topic("RecordHeadersOutputTopic", Serdes.String, Serdes.String)

  test("outputs records if their header value matches") {
    val headers = new RecordHeaders()
    val recordHeader = new RecordHeader("should-forward", Array(MatchingHeaderValue.toByte))
    headers.add(recordHeader)

    recordHeadersTopic.pipeInputWithHeaders("key", "value1", headers)

    recordHeadersOutputTopic.assertOutput("key", "value1")
  }

  test("does not output records if their header value does not match") {
    val headers = new RecordHeaders()
    val recordHeader = new RecordHeader("should-forward", Array(NonMatchingHeaderValue.toByte))
    headers.add(recordHeader)

    recordHeadersTopic.pipeInputWithHeaders("key", "value1", headers)

    assert(recordHeadersOutputTopic.readAllOutput().isEmpty)
  }

  test("does not output records if no headers specified") {
    val headers = new RecordHeaders() // this collection is empty

    recordHeadersTopic.pipeInputWithHeaders("key", "value1", headers)

    assert(recordHeadersOutputTopic.readAllOutput().isEmpty)
  }

  test("does not output records if no matching headers found") {
    val headers = new RecordHeaders()
    val recordHeader = new RecordHeader("different-header", Array(NonMatchingHeaderValue.toByte))
    headers.add(recordHeader)

    recordHeadersTopic.pipeInputWithHeaders("key", "value1", headers)

    assert(recordHeadersOutputTopic.readAllOutput().isEmpty)
  }
}
