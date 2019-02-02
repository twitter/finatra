package com.twitter.finatra.kafkastreams.integration.wordcount

import com.twitter.finatra.kafka.serde.ScalaSerdes
import com.twitter.finatra.kafkastreams.test.{FinatraTopologyTester, TopologyFeatureTest}
import org.apache.kafka.common.serialization.Serdes
import org.joda.time.DateTime

class WordCountServerTopologyFeatureTest extends TopologyFeatureTest {

  override val topologyTester = FinatraTopologyTester(
    kafkaApplicationId = "wordcount-prod-bob",
    server = new WordCountRocksDbServer,
    startingWallClockTime = new DateTime("2018-01-01T00:00:00Z")
  )

  private val textLinesTopic =
    topologyTester.topic("TextLinesTopic", Serdes.ByteArray(), Serdes.String)

  private val wordsWithCountsTopic =
    topologyTester.topic("WordsWithCountsTopic", Serdes.String, ScalaSerdes.Long)

  test("word count test 1") {
    val countsStore = topologyTester.getKeyValueStore[String, Long]("CountsStore")

    textLinesTopic.pipeInput(Array.emptyByteArray, "Hello World Hello")

    wordsWithCountsTopic.assertOutput("Hello", 1)
    wordsWithCountsTopic.assertOutput("World", 1)
    wordsWithCountsTopic.assertOutput("Hello", 2)

    countsStore.get("Hello") should equal(2)
    countsStore.get("World") should equal(1)
  }

  test("word count test 2") {
    val countsStore = topologyTester.getKeyValueStore[String, Long]("CountsStore")

    textLinesTopic.pipeInput(Array.emptyByteArray, "yo yo yo")

    wordsWithCountsTopic.assertOutput("yo", 1)
    wordsWithCountsTopic.assertOutput("yo", 2)
    wordsWithCountsTopic.assertOutput("yo", 3)

    countsStore.get("yo") should equal(3)
  }
}
