package com.twitter.finatra.kafkastreams.integration.globaltable

import com.twitter.finatra.kafka.serde.ScalaSerdes
import com.twitter.finatra.kafkastreams.config.KafkaStreamsConfig
import com.twitter.finatra.kafkastreams.test.KafkaStreamsMultiServerFeatureTest
import com.twitter.inject.server.EmbeddedTwitterServer

class GlobalTableServerFeatureTest extends KafkaStreamsMultiServerFeatureTest {

  private val globalTableClientIdPatterns = Set("global-consumer", "GlobalStreamThread")

  kafkaTopic(ScalaSerdes.Int, ScalaSerdes.Int, GlobalTableServer.GlobalTableTopic)

  test("verify globalTable metrics included") {
    val server = new EmbeddedTwitterServer(
      new GlobalTableServer {
        override def streamsProperties(config: KafkaStreamsConfig): KafkaStreamsConfig = {
          super
            .streamsProperties(config)
            .withConfig("includeGlobalTableMetrics", "true")
        }
      },
      flags = kafkaStreamsFlags ++ Map("kafka.application.id" -> "GlobalTableServer"),
      disableTestLogging = true
    )

    assert(server.inMemoryStats.gauges.toSortedMap.keys.exists { metric =>
      globalTableClientIdPatterns.exists(metric.contains)
    })
    server.close()
  }

  test("verify globalTable metrics filtered") {
    val server = new EmbeddedTwitterServer(
      new GlobalTableServer {
        override def streamsProperties(config: KafkaStreamsConfig): KafkaStreamsConfig = {
          super
            .streamsProperties(config)
            .withConfig("includeGlobalTableMetrics", "false")
        }
      },
      flags = kafkaStreamsFlags ++ Map("kafka.application.id" -> "GlobalTableServer"),
      disableTestLogging = true
    )

    assert(!server.inMemoryStats.gauges.toSortedMap.keys.exists { metric =>
      globalTableClientIdPatterns.exists(metric.contains)
    })
    server.close()
  }
}
