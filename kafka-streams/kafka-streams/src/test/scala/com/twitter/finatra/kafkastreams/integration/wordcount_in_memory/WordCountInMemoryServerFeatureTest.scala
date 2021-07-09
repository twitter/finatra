package com.twitter.finatra.kafkastreams.integration.wordcount_in_memory

import com.twitter.conversions.DurationOps._
import com.twitter.finatra.kafka.serde.ScalaSerdes
import com.twitter.finatra.kafkastreams.test.KafkaStreamsFeatureTest
import com.twitter.inject.server.EmbeddedTwitterServer
import org.apache.kafka.common.serialization.Serdes

class WordCountInMemoryServerFeatureTest extends KafkaStreamsFeatureTest {

  override val server = new EmbeddedTwitterServer(
    new WordCountInMemoryServer,
    flags = kafkaStreamsFlags ++ Map("kafka.application.id" -> "wordcount-prod"),
    disableTestLogging = true
  )

  private val textLinesTopic = kafkaTopic(ScalaSerdes.Long, Serdes.String, "text-lines-topic")
  private val wordsWithCountsTopic = kafkaTopic(Serdes.String, Serdes.Long, "WordsWithCountsTopic")

  if (!sys.props.contains("SKIP_FLAKY_TRAVIS")) {
    test("word count") {
      server.start()

      textLinesTopic.publish(1L -> "hello world hello")
      server.inMemoryStats.gauges
        .waitFor("kafka/thread1/consumer/text_lines_topic/records_consumed_total", 1, 60.seconds)
      wordsWithCountsTopic.consumeMessages(numMessages = 3) should contain theSameElementsAs Seq(
        "world" -> 1,
        "hello" -> 1,
        "hello" -> 2
      )
      server.inMemoryStats.gauges.assert(
        "kafka/thread1/producer/wordcount_prod_CountsStore_changelog/record_send_total",
        3.0f
      )
      server.inMemoryStats.gauges.assert(
        "kafka/thread1/producer/WordsWithCountsTopic/record_send_total",
        3.0f
      )

      textLinesTopic.publish(1L -> "world world")
      waitForKafkaMetric("kafka/thread1/consumer/text_lines_topic/records_consumed_total", 2)
      wordsWithCountsTopic.consumeMessages(numMessages = 2) should contain theSameElementsAs Seq(
        "world" -> 2,
        "world" -> 3
      )
      server.inMemoryStats.gauges.assert(
        "kafka/thread1/producer/wordcount_prod_CountsStore_changelog/record_send_total",
        5.0f
      )
      server.inMemoryStats.gauges.assert(
        "kafka/thread1/producer/WordsWithCountsTopic/record_send_total",
        5.0f
      )
      server.inMemoryStats.gauges.assert(
        "kafka/stream/state",
        2.0f
      )

      assert(
        server.inMemoryStats.stats
          .get("kafka/consumer/text-lines-topic/time_since_record_timestamp_ms").isEmpty)
      assert(
        server.inMemoryStats.stats
          .get("kafka/consumer/text_lines_topic/time_since_record_timestamp_ms").isDefined)
    }
  }
}
