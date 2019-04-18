package com.twitter.finatra.kafkastreams.integration.wordcount

import com.twitter.finatra.kafka.serde.ScalaSerdes
import com.twitter.finatra.kafka.test.utils.InMemoryStatsUtil
import com.twitter.finatra.kafkastreams.config.KafkaStreamsConfig
import com.twitter.finatra.kafkastreams.internal.stats.KafkaStreamsFinagleMetricsReporter
import com.twitter.finatra.kafkastreams.test.KafkaStreamsMultiServerFeatureTest
import com.twitter.inject.server.EmbeddedTwitterServer
import com.twitter.util.Await
import org.apache.kafka.common.metrics.Sensor.RecordingLevel
import org.apache.kafka.common.serialization.Serdes

class WordCountServerFeatureTest extends KafkaStreamsMultiServerFeatureTest {

  private def createServer(
    recordingLevel: RecordingLevel = RecordingLevel.INFO
  ): EmbeddedTwitterServer = {
    new EmbeddedTwitterServer(
      new WordCountRocksDbServer {
        override def streamsProperties(config: KafkaStreamsConfig): KafkaStreamsConfig = {
          super
            .streamsProperties(config)
            .metricsRecordingLevelConfig(recordingLevel)
        }
      },
      flags = kafkaStreamsFlags ++ Map("kafka.application.id" -> "wordcount-prod")
    )
  }

  override def beforeEach(): Unit = {
    resetStreamThreadId()
  }

  private val textLinesTopic = kafkaTopic(ScalaSerdes.Long, Serdes.String, "TextLinesTopic")
  private val countsChangelogTopic = kafkaTopic(
    Serdes.String,
    Serdes.Long,
    "wordcount-prod-CountsStore-changelog",
    autoCreate = false
  )
  private val wordsWithCountsTopic = kafkaTopic(Serdes.String, Serdes.Long, "WordsWithCountsTopic")

  test("word count") {
    val serverBeforeRestart = createServer()
    val serverBeforeRestartStats = InMemoryStatsUtil(serverBeforeRestart.injector)
    serverBeforeRestart.start()

    textLinesTopic.publish(1L -> "hello world hello")
    serverBeforeRestartStats.waitForGauge(
      "kafka/thread1/consumer/TextLinesTopic/records_consumed_total",
      1
    )
    wordsWithCountsTopic.consumeAsManyMessagesUntilMap(Map("world" -> 1L, "hello" -> 2L))

    textLinesTopic.publish(1L -> "world world")
    serverBeforeRestartStats.waitForGauge(
      "kafka/thread1/consumer/TextLinesTopic/records_consumed_total",
      2
    )
    wordsWithCountsTopic.consumeAsManyMessagesUntilMap(Map("world" -> 3L))
    serverBeforeRestartStats.waitForGaugeUntil(
      "kafka/thread1/producer/wordcount_prod_CountsStore_changelog/record_send_total",
      _ >= 3
    )
    serverBeforeRestartStats.waitForGaugeUntil(
      "kafka/thread1/producer/WordsWithCountsTopic/record_send_total",
      _ >= 3
    )

    serverBeforeRestart.assertGauge("kafka/stream/state", 2)
    assert(countsChangelogTopic.consumeValue() > 0)
    assert(
      serverBeforeRestart
        .getGauge("kafka/thread1/producer/wordcount_prod_CountsStore_changelog/byte_total") > 0
    )
    assertTimeSincePublishedSet(
      serverBeforeRestart,
      "kafka/consumer/TextLinesTopic/time_since_record_published_ms"
    )
    assertTimeSincePublishedSet(
      serverBeforeRestart,
      "kafka/consumer/wordcount-prod-CountsStore-repartition/time_since_record_published_ms"
    )

    serverBeforeRestart.printStats()
    serverBeforeRestart.close()

    val serverAfterRestart = createServer()
    serverAfterRestart.start()
    val serverAfterRestartStats = InMemoryStatsUtil(serverAfterRestart.injector)
    serverAfterRestartStats.waitForGaugeUntil(
      "kafka/stream/finatra_state_restore_listener/restore_time_elapsed_ms",
      _ >= 0
    )

    textLinesTopic.publish(1L -> "world world")
    wordsWithCountsTopic.consumeAsManyMessagesUntilMap(Map("world" -> 5L))

    // Why isn't the records_consumed_total stat > 0 in the feature test?
    // val serverAfterRestartStats = new InMemoryStatsUtil(serverAfterRestart.injector)
    // serverAfterRestartStats.waitForGaugeUntil("kafka/thread2/restore_consumer/records_consumed_total", _ > 0)
    serverAfterRestart.getStat(
      "kafka/consumer/wordcount-prod-CountsStore-changelog/time_since_record_published_ms"
    ) should equal(Seq())
    serverAfterRestart.getStat(
      "kafka/consumer/wordcount-prod-CountsStore-changelog/time_since_record_timestamp_ms"
    ) should equal(Seq())
    serverAfterRestart.close()
    Await.result(serverAfterRestart.mainResult)
  }

  test("ensure debug metrics not included if RecordingLevel is not DEBUG") {
    val debugServer = createServer(recordingLevel = RecordingLevel.DEBUG)
    debugServer.start()
    val debugServerStats = InMemoryStatsUtil(debugServer.injector)

    textLinesTopic.publish(1L -> "hello world hello")
    debugServerStats.waitForGauge("kafka/thread1/consumer/TextLinesTopic/records_consumed_total", 1)
    val debugServerMetricNames = debugServerStats.metricNames

    debugServer.close()
    resetStreamThreadId()

    val infoServer = createServer(recordingLevel = RecordingLevel.INFO)
    infoServer.start()
    val infoServerStats = InMemoryStatsUtil(infoServer.injector)

    textLinesTopic.publish(1L -> "hello world hello")
    infoServerStats.waitForGauge("kafka/thread1/consumer/TextLinesTopic/records_consumed_total", 1)
    val infoServerMetricNames = infoServerStats.metricNames

    infoServer.close()

    // `infoServer` should not contain DEBUG-level ''rocksdb'' metrics
    assert(debugServerMetricNames.exists(_.contains("rocksdb")))
    assert(!infoServerMetricNames.exists(_.contains("rocksdb")))
    assert(!infoServerMetricNames.exists { metric =>
      KafkaStreamsFinagleMetricsReporter.debugMetrics.exists(metric.endsWith)
    })
    assert(debugServerMetricNames.size > infoServerMetricNames.size)
  }

  /* Private */

  private def assertTimeSincePublishedSet(server: EmbeddedTwitterServer, topic: String): Unit = {
    assert(
      server.getStat(topic).nonEmpty &&
        server.getStat(topic).forall(_ >= 0)
    )
  }
}
