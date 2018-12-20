package com.twitter.unittests.integration.stateless

import com.twitter.finatra.kafka.serde.ScalaSerdes
import com.twitter.finatra.kafkastreams.test.KafkaStreamsMultiServerFeatureTest
import com.twitter.inject.server.EmbeddedTwitterServer
import org.apache.kafka.common.serialization.Serdes

class VerifyFailureServerFeatureTest extends KafkaStreamsMultiServerFeatureTest {

  kafkaTopic(ScalaSerdes.Long, Serdes.String, "TextLinesTopic")
  kafkaTopic(Serdes.String, Serdes.Long, "WordsWithCountsTopic")

  test("verify stateful server will fail") {
    val server = new EmbeddedTwitterServer(
      new VerifyFailureServer,
      flags = kafkaStreamsFlags ++ Map("kafka.application.id" -> "VerifyFailureServer")
    )

    intercept[UnsupportedOperationException] {
      server.start()
    }

    server.close()
  }
}
