package com.twitter.finatra.kafkastreams.integration.wordcount_in_memory

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

  private val textLinesTopic = kafkaTopic(ScalaSerdes.Long, Serdes.String, "TextLinesTopic")
  private val wordsWithCountsTopic = kafkaTopic(Serdes.String, Serdes.Long, "WordsWithCountsTopic")

  test("word count") {
    server.start()

    textLinesTopic.publish(1L -> "hello world hello")
    waitForKafkaMetric("kafka/thread1/consumer/TextLinesTopic/records_consumed_total", 1)
    wordsWithCountsTopic.consumeMessages(numMessages = 3) should contain theSameElementsAs Seq(
      "world" -> 1,
      "hello" -> 1,
      "hello" -> 2
    )
    server.assertGauge(
      "kafka/thread1/producer/wordcount_prod_CountsStore_changelog/record_send_total",
      3
    )
    server.assertGauge("kafka/thread1/producer/WordsWithCountsTopic/record_send_total", 3)

    textLinesTopic.publish(1L -> "world world")
    waitForKafkaMetric("kafka/thread1/consumer/TextLinesTopic/records_consumed_total", 2)
    wordsWithCountsTopic.consumeMessages(numMessages = 2) should contain theSameElementsAs Seq(
      "world" -> 2,
      "world" -> 3
    )
    server.assertGauge(
      "kafka/thread1/producer/wordcount_prod_CountsStore_changelog/record_send_total",
      5
    )
    server.assertGauge("kafka/thread1/producer/WordsWithCountsTopic/record_send_total", 5)

    server.assertGauge("kafka/stream/state", 2)
    server.printStats()
  }
}
