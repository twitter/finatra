package com.twitter.finatra.kafka.consumers

import com.twitter.finagle.tracing.Tracing
import org.apache.kafka.clients.consumer.ConsumerRecords

/**
 * A service to add trace annotations to the Kafka consumer's `poll` span.
 */
trait KafkaConsumerTraceAnnotator {

  /**
   * Record annotations to Kafka consumer's `poll` span.
   *
   * @param trace Kafka consumer's `poll` span which is actively tracing.
   * @param records the records consumed from the `poll` operation.
   * @param consumerConfig the config of the Kafka consumer.
   * @tparam K type of Key.
   * @tparam V type of Value.
   */
  def recordAnnotations[K, V](
    trace: Tracing,
    records: ConsumerRecords[K, V],
    consumerConfig: Map[String, AnyRef]
  ): Unit
}
