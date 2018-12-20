package com.twitter.unittests.integration.async_transformer

import com.twitter.finatra.kafka.serde.ScalaSerdes
import com.twitter.finatra.kafkastreams.test.KafkaStreamsFeatureTest
import com.twitter.inject.conversions.time._
import com.twitter.inject.server.EmbeddedTwitterServer
import com.twitter.util.Try
import org.apache.kafka.common.serialization.Serdes

class WordLookupAsyncServerFeatureTest extends KafkaStreamsFeatureTest {

  override val server = new EmbeddedTwitterServer(
    new WordLookupAsyncServer,
    flags = kafkaStreamsFlags ++ Map("kafka.application.id" -> "wordcount-prod")
  )

  private val textLinesTopic = kafkaTopic(
    ScalaSerdes.Long,
    Serdes.String,
    "TextLinesTopic",
    logPublish = true,
    autoConsume = false
  )
  private val wordsWithCountsTopic = kafkaTopic(Serdes.String, Serdes.Long, "WordToWordLength")

  test("word count") {
    server.start()
    textLinesTopic.publish(1L -> "hello world foo")
    wordsWithCountsTopic.consumeExpectedMap(Map("hello" -> 5L, "world" -> 5L, "foo" -> 3L))

    val otherResults =
      Try(wordsWithCountsTopic.consumeMessages(numMessages = 1, 2.seconds)).getOrElse(Seq.empty)
    otherResults should equal(Seq.empty) // make sure there are no more results except those we have seen so far!
  }
}
