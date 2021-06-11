package com.twitter.finatra.kafkastreams.integration.async_transformer

import com.twitter.conversions.DurationOps._
import com.twitter.finatra.kafka.serde.ScalaSerdes
import com.twitter.finatra.kafkastreams.test.KafkaStreamsFeatureTest
import com.twitter.inject.server.EmbeddedTwitterServer
import com.twitter.util.Try
import com.twitter.util.jackson.JsonDiff
import java.lang.{Long => JLong}
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.common.header.Header
import org.apache.kafka.common.header.internals.RecordHeader
import org.apache.kafka.common.serialization.Serdes
import org.scalacheck.Gen

class WordLookupAsyncServerFeatureTestBase extends KafkaStreamsFeatureTest {

  override val server = new EmbeddedTwitterServer(
    new WordLookupAsyncServer,
    flags = kafkaStreamsFlags ++ Map(
      "kafka.application.id" -> "wordcount-prod",
      "kafka.commit.interval" -> "30.seconds"
    ),
    disableTestLogging = true
  )

  private val textLinesTopic = kafkaTopic(
    ScalaSerdes.Long,
    Serdes.String,
    "TextLinesTopic",
    logPublish = true
  )
  private val wordsWithCountsTopic = kafkaTopic(Serdes.String, Serdes.Long, "WordToWordLength")

  private val recordHeaderKeyGen: Gen[String] = Gen.alphaNumStr
  private val recordHeaderValueGen: Gen[String] = Gen.alphaNumStr
  private val recordHeaderGen: Gen[RecordHeader] = for {
    recordHeaderkey <- recordHeaderKeyGen
    recordHeaderValue <- recordHeaderValueGen
  } yield {
    new RecordHeader(recordHeaderkey, recordHeaderValue.getBytes)
  }

  val recordHeaders1 = Seq(recordHeaderGen.sample.get)
  val recordHeaders2 = Seq(recordHeaderGen.sample.get, recordHeaderGen.sample.get)
  val recordHeaders3 =
    Seq(recordHeaderGen.sample.get, recordHeaderGen.sample.get, recordHeaderGen.sample.get)

  test("word count") {
    server.start()
    textLinesTopic.publish(1L -> "hello world foo")
    wordsWithCountsTopic.consumeExpectedMap(Map("hello" -> 5L, "world" -> 5L, "foo" -> 3L))

    assertOutputTopicEmpty()
  }

  test("Headers should be preserved and passed downstream") {
    textLinesTopic.publish(
      keyValue = 1L -> "the quick brown fox",
      timestamp = 1000L,
      headers = recordHeaders1
    )
    assertOutputRecordsAndHeaders(
      expectedKeyValueMap = Map("the" -> 3L, "quick" -> 5L, "brown" -> 5L, "fox" -> 3L),
      expectedHeaders = recordHeaders1
    )

    textLinesTopic.publish(
      keyValue = 2L -> "jumps over",
      timestamp = 2000L,
      headers = recordHeaders2
    )
    assertOutputRecordsAndHeaders(
      expectedKeyValueMap = Map("jumps" -> 5L, "over" -> 4L),
      expectedHeaders = recordHeaders2
    )

    textLinesTopic.publish(
      keyValue = 3L -> "the lazy dog",
      timestamp = 3000L,
      headers = recordHeaders3
    )
    assertOutputRecordsAndHeaders(
      expectedKeyValueMap = Map("the" -> 3L, "lazy" -> 4L, "dog" -> 3L),
      expectedHeaders = recordHeaders3
    )

    assertOutputTopicEmpty()
  }

  test("Headers should be preserved for out-of-order future completes") {
    textLinesTopic.publish(
      keyValue = 1L -> "hello|5",
      timestamp = 1000L,
      headers = recordHeaders1
    )

    textLinesTopic.publish(
      keyValue = 2L -> "world|3",
      timestamp = 1000L,
      headers = recordHeaders2
    )

    textLinesTopic.publish(
      keyValue = 3L -> "foo|1",
      timestamp = 1000L,
      headers = recordHeaders3
    )

    server.inMemoryStats.gauges.waitFor("kafka/stream/outstandingFutures", 3, 5.seconds)
    server.inMemoryStats.gauges.assert("kafka/stream/outstandingResults", 0)

    assertOutputRecordsAndHeaders(
      expectedKeyValueMap = Map("foo|1" -> 3L),
      expectedHeaders = recordHeaders3
    )

    assertOutputRecordsAndHeaders(
      expectedKeyValueMap = Map("world|3" -> 5L),
      expectedHeaders = recordHeaders2
    )

    assertOutputRecordsAndHeaders(
      expectedKeyValueMap = Map("hello|5" -> 5L),
      expectedHeaders = recordHeaders1
    )

    server.inMemoryStats.gauges.assert("kafka/stream/outstandingFutures", 0)
    server.inMemoryStats.gauges.assert("kafka/stream/outstandingResults", 0)

    assertOutputTopicEmpty()
  }

  private def assertOutputRecordsAndHeaders(
    expectedKeyValueMap: Map[String, JLong],
    expectedHeaders: Seq[Header]
  ): Unit = {
    val outputRecords: Seq[ConsumerRecord[String, JLong]] =
      (0 until expectedKeyValueMap.size).map(_ => wordsWithCountsTopic.consumeRecord())

    val outputKeyValues: Map[String, JLong] =
      outputRecords.map(record => (record.key(), record.value())).toMap
    JsonDiff.assertDiff(expectedKeyValueMap, outputKeyValues)

    expectedHeaders.forall { expectedHeader =>
      outputRecords.forall { record =>
        val header = record.headers().lastHeader(expectedHeader.key())
        header.value().sameElements(expectedHeader.value())
      }
    } should be(true)
  }

  private def assertOutputTopicEmpty(): Unit = {
    val otherResults =
      Try(wordsWithCountsTopic.consumeMessages(numMessages = 1, 2.seconds)).getOrElse(Seq.empty)
    otherResults should equal(Seq.empty)
  }
}
