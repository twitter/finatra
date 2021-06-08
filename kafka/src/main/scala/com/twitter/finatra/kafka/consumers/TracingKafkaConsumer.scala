package com.twitter.finatra.kafka.consumers

import java.time.Duration
import java.util.Properties
import org.apache.kafka.clients.consumer.{ConsumerRecords, KafkaConsumer}
import org.apache.kafka.common.serialization.Deserializer
import scala.collection.JavaConverters._

object TracingKafkaConsumer {

  /**
   * Helper constructor which accepts [[Properties]] for constructing the [[TracingKafkaConsumer]].
   *
   * @param configs The consumer configs.
   * @param keyDeserializer The serializer for key that implements [[Deserializer]]. The <pre>configure()</pre>
   *                        method won't be called in the consumer when the deserializer is passed in
   *                        directly.
   * @param valueDeserializer The serializer for value that implements [[Deserializer]]. The
   *                          <pre>configure()</pre> method won't be called in the consumer when the
   *                          deserializer is passed in directly.
   * @tparam K type of Key.
   * @tparam V type of value.
   * @return an instance of [[TracingKafkaConsumer]].
   */
  @deprecated(
    "There is no way to propagate the TraceId back to the caller without changing the API, so we recommend the users interested in tracing manually wrap the results of the `poll` method with TracingKafkaConsumer.trace(records) { ... } and use the callback to do further processing which can be traced.",
    "2021-05-06"
  )
  def apply[K, V](
    configs: Properties,
    keyDeserializer: Deserializer[K],
    valueDeserializer: Deserializer[V]
  ): TracingKafkaConsumer[K, V] = {
    new TracingKafkaConsumer(configs.asScala.toMap, keyDeserializer, valueDeserializer)
  }
}

/**
 * An extension of [[KafkaConsumer KafkaConsumer]] with Zipkin tracing to trace the records received
 * from Kafka. This is inspired by openzipkin's <a href="https://github.com/openzipkin/brave">brave</a>
 * instrumentation.
 *
 * @param configs The consumer configs.
 * @param keyDeserializer The serializer for key that implements [[Deserializer]]. The <pre>configure()</pre>
 *                        method won't be called in the consumer when the deserializer is passed in
 *                        directly.
 * @param valueDeserializer The serializer for value that implements [[Deserializer]]. The
 *                          <pre>configure()</pre> method won't be called in the consumer when the
 *                          deserializer is passed in directly.
 * @tparam K type of Key.
 * @tparam V type of value.
 */
@deprecated(
  "There is no way to propagate the TraceId back to the caller without changing the API we recommend the users interested in tracing manually wrap the `poll` method with TracingKafkaConsumer.trace(records) { ... } and use the callback to do further processing which can be traced.",
  "2021-05-06"
)
class TracingKafkaConsumer[K, V](
  configs: Map[String, AnyRef],
  keyDeserializer: Deserializer[K],
  valueDeserializer: Deserializer[V])
    extends KafkaConsumer[K, V](configs.asJava, keyDeserializer, valueDeserializer) {

  override def poll(timeout: Duration): ConsumerRecords[K, V] = {
    val records = super.poll(timeout)
    // NOTE: Currently this is mostly going to create single span traces and the trace id might
    // not propagate back to the caller.
    KafkaConsumerTracer.trace(records, configs) {}
    records
  }
}
