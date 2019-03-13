package com.twitter.finatra.kafka.test.integration

import com.twitter.finagle.stats.{NullStatsReceiver, StatsReceiver}
import com.twitter.finatra.kafka.domain.AckMode
import com.twitter.finatra.kafka.producers.FinagleKafkaProducerBuilder
import com.twitter.finatra.kafka.stats.KafkaFinagleMetricsReporter
import com.twitter.finatra.kafka.test.EmbeddedKafka
import com.twitter.finatra.kafka.test.utils.InMemoryStatsUtil
import com.twitter.inject.app.TestInjector
import com.twitter.inject.modules.InMemoryStatsReceiverModule
import com.twitter.util.{Await, Duration, Time}
import org.apache.kafka.common.serialization.Serdes

class FinagleKafkaProducerIntegrationTest extends EmbeddedKafka {

  private val testTopic = kafkaTopic(Serdes.String, Serdes.String, "test-topic")

  private def getTestProducer(statsReceiver: StatsReceiver) = {
    FinagleKafkaProducerBuilder()
      .dest(brokers.map(_.brokerList()).mkString(","))
      .statsReceiver(statsReceiver)
      .clientId("test-producer")
      .ackMode(AckMode.ALL)
      .keySerializer(Serdes.String.serializer)
      .valueSerializer(Serdes.String.serializer)
      .build()
  }

  test("success then failure publish") {
    val injector = TestInjector(InMemoryStatsReceiverModule).create
    KafkaFinagleMetricsReporter.init(injector)

    val producer = getTestProducer(injector.instance[StatsReceiver])

    try {
      Await.result(producer.send("test-topic", "Foo", "Bar", System.currentTimeMillis))

      val statsUtils = InMemoryStatsUtil(injector)
      statsUtils.printStats()

      statsUtils.assertGauge("kafka/test_producer/record_send_total", 1)
      val onSendLag = statsUtils.getStat("kafka/test_producer/record_timestamp_on_send_lag")
      assert(onSendLag.size == 1)
      assert(onSendLag.head >= 0)

      val onSuccessLag = statsUtils.getStat("kafka/test_producer/record_timestamp_on_success_lag")
      assert(onSuccessLag.size == 1)
      assert(onSuccessLag.head >= onSendLag.head)

      /* Stop the brokers so that the next publish attempt results in publish error */
      closeEmbeddedKafka()

      intercept[org.apache.kafka.common.errors.TimeoutException] {
        Await.result(producer.send("test-topic", "Hello", "World", System.currentTimeMillis))
      }

      statsUtils.printStats()
      statsUtils.assertGauge("kafka/test_producer/record_error_total", 1)
      val onFailureLag = statsUtils.getStat("kafka/test_producer/record_timestamp_on_failure_lag")
      assert(onFailureLag.size == 1)
      assert(onFailureLag.head >= 0)
    } finally {
      producer.close()
    }
  }

  test("close with Time.Bottom deadline should not throw exception") {
    val producer = getTestProducer(NullStatsReceiver)
    await(producer.close(Time.Bottom))
  }

  test("close with valid deadline should not throw exception") {
    val producer = getTestProducer(NullStatsReceiver)
    await(producer.close(Time.now + Duration.fromSeconds(10)))
  }

}
