package com.twitter.finatra.kafka.producers

import com.twitter.finagle.tracing.Tracing
import org.apache.kafka.clients.producer.ProducerRecord

/**
 * A service to add annotations to the Kafka producer's `send` span.
 */
trait KafkaProducerTraceAnnotator {

  /**
   * Record trace annotations in the Kafka producer's `send` span.
   *
   * @param trace Kafka producer's `send` span which is actively tracing.
   * @param record producer record being sent.
   * @param producerConfig Kafka producer config.
   * @tparam K type of Key.
   * @tparam V type of Value.
   */
  def recordAnnotations[K, V](
    trace: Tracing,
    record: ProducerRecord[K, V],
    producerConfig: Map[String, AnyRef]
  ): Unit
}
