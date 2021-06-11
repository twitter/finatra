package com.twitter.finatra.kafkastreams.integration.wordcount

import com.twitter.conversions.DurationOps._
import com.twitter.finatra.kafka.serde.ScalaSerdes
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
      flags = kafkaStreamsFlags ++ Map("kafka.application.id" -> "wordcount-prod"),
      disableTestLogging = true
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
    val serverBeforeRestartStats = serverBeforeRestart.inMemoryStats
    serverBeforeRestart.start()

    textLinesTopic.publish(1L -> "hello world hello")
    serverBeforeRestartStats.gauges.waitFor(
      "kafka/thread1/consumer/TextLinesTopic/records_consumed_total",
      1.0f,
      60.seconds)
    wordsWithCountsTopic.consumeAsManyMessagesUntilMap(Map("world" -> 1L, "hello" -> 2L))

    textLinesTopic.publish(1L -> "world world")
    serverBeforeRestartStats.gauges.waitFor(
      "kafka/thread1/consumer/TextLinesTopic/records_consumed_total",
      2.0f,
      60.seconds)
    wordsWithCountsTopic.consumeAsManyMessagesUntilMap(Map("world" -> 3L))
    serverBeforeRestartStats.gauges.waitFor(
      "kafka/thread1/producer/wordcount_prod_CountsStore_changelog/record_send_total",
      60.seconds)(_ >= 3.0f)
    serverBeforeRestartStats.gauges.waitFor(
      "kafka/thread1/producer/WordsWithCountsTopic/record_send_total",
      60.seconds)(_ >= 3.0f)

    serverBeforeRestartStats.gauges.assert("kafka/stream/state", 2.0f)
    assert(countsChangelogTopic.consumeValue() > 0)
    assert(
      serverBeforeRestartStats
        .gauges("kafka/thread1/producer/wordcount_prod_CountsStore_changelog/byte_total") > 0.0f
    )
    assertTimeSincePublishedSet(
      serverBeforeRestart,
      "kafka/consumer/TextLinesTopic/time_since_record_published_ms"
    )
    assertTimeSincePublishedSet(
      serverBeforeRestart,
      "kafka/consumer/wordcount_prod_CountsStore_repartition/time_since_record_published_ms"
    )

    serverBeforeRestart.close()

    val serverAfterRestart = createServer()
    serverAfterRestart.start()
    val serverAfterRestartStats = serverAfterRestart.inMemoryStats
    serverAfterRestartStats.gauges.waitFor(
      "kafka/stream/finatra_state_restore_listener/restore_time_elapsed_ms",
      60.seconds)(_ >= 0.0f)

    textLinesTopic.publish(1L -> "world world")
    wordsWithCountsTopic.consumeAsManyMessagesUntilMap(Map("world" -> 5L))

    // Why isn't the records_consumed_total stat > 0 in the feature test?
    // val serverAfterRestartStats = new InMemoryStatsUtil(serverAfterRestart.injector)
    // serverAfterRestartStats.gauges.waitForUntil("kafka/thread2/restore_consumer/records_consumed_total", _ > 0)
    serverAfterRestartStats.stats.get(
      "kafka/consumer/wordcount_prod_CountsStore_changelog/time_since_record_published_ms"
    ) should be(None)
    serverAfterRestartStats.stats.get(
      "kafka/consumer/wordcount_prod_CountsStore_changelog/time_since_record_timestamp_ms"
    ) should be(None)
    serverAfterRestart.close()
    Await.result(serverAfterRestart.mainResult)
  }

  test("ensure debug metrics not included if RecordingLevel is not DEBUG") {
    val debugServer = createServer(recordingLevel = RecordingLevel.DEBUG)
    debugServer.start()
    val debugServerStats = debugServer.inMemoryStats

    textLinesTopic.publish(1L -> "hello world hello")
    debugServerStats.gauges.waitFor(
      "kafka/thread1/consumer/TextLinesTopic/records_consumed_total",
      1.0f,
      60.seconds)
    val debugServerMetricNames = debugServerStats.names

    debugServer.close()
    resetStreamThreadId()

    val infoServer = createServer(recordingLevel = RecordingLevel.INFO)
    infoServer.start()
    val infoServerStats = infoServer.inMemoryStats

    textLinesTopic.publish(1L -> "hello world hello")
    infoServerStats.gauges.waitFor(
      "kafka/thread1/consumer/TextLinesTopic/records_consumed_total",
      1.0f,
      60.seconds)
    val infoServerMetricNames = infoServerStats.names

    infoServer.close()

    // `infoServer` should not contain DEBUG-level ''rocksdb'' metrics
    // There are some significant changes between Kafka 2.2 and 2.4+ about the rocksDB metrics.
    // assert(debugServerMetricNames.exists(_.contains("rocksdb")))
    assert(!infoServerMetricNames.exists(_.contains("rocksdb")))
    assert(!infoServerMetricNames.exists { metric =>
      KafkaStreamsFinagleMetricsReporter.debugMetrics.exists(metric.endsWith)
    })
    assert(debugServerMetricNames.size > infoServerMetricNames.size)
  }

  /* Private */

  private def assertTimeSincePublishedSet(server: EmbeddedTwitterServer, topic: String): Unit = {
    assert(
      server.inMemoryStats.stats.get(topic).nonEmpty &&
        server.inMemoryStats.stats(topic).forall(_ >= 0.0f)
    )
  }
}
