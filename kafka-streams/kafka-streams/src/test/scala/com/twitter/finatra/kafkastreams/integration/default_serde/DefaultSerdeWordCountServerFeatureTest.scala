package com.twitter.finatra.kafkastreams.integration.default_serde

import com.twitter.finatra.kafka.serde.ScalaSerdes
import com.twitter.finatra.kafkastreams.KafkaStreamsTwitterServer
import com.twitter.finatra.kafkastreams.test.KafkaStreamsFeatureTest
import com.twitter.inject.server.EmbeddedTwitterServer
import com.twitter.util.Await
import org.apache.kafka.common.serialization.Serdes

class DefaultSerdeWordCountServerFeatureTest extends KafkaStreamsFeatureTest {

  override val server = new EmbeddedTwitterServer(
    new DefaultSerdeWordCountDbServer,
    flags = kafkaStreamsFlags ++ Map("kafka.application.id" -> "wordcount-prod"),
    disableTestLogging = true
  )

  private val textLinesTopic = kafkaTopic(ScalaSerdes.Long, Serdes.String, "TextLinesTopic")
  private val countsChangelogTopic = kafkaTopic(
    Serdes.String,
    Serdes.Long,
    "wordcount-prod-CountsStore-changelog",
    autoCreate = false
  )
  private val wordsWithCountsTopic = kafkaTopic(Serdes.String, Serdes.Long, "WordsWithCountsTopic")

  test("word count") {
    server.start()
    textLinesTopic.publish(1L -> "hello world hello")
    Await.result(server.mainResult)
    server.injectableServer
      .asInstanceOf[KafkaStreamsTwitterServer].uncaughtException.toString should include(
      "Default Deserializer's should be avoided "
    )
  }
}
