package com.twitter.finatra.kafkastreams.internal.interceptors

import com.twitter.finatra.kafka.interceptors.MonitoringConsumerInterceptor

/**
 * An Kafka Streams aware interceptor that looks for the `publish_time` header and record timestamp and calculates
 * how much time has passed since the each of those times and updates stats for each.
 *
 * Note: Since this interceptor is Kafka Streams aware, it will not calculate stats when reading changelog topics to restore
 * state, since this has been shown to be a hot-spot during restoration of large amounts of state.
 */
private[kafkastreams] class KafkaStreamsMonitoringConsumerInterceptor
    extends MonitoringConsumerInterceptor {

  /**
   * Determines if this interceptor should be enabled given the consumer client id
   */
  override protected def enableInterceptorForClientId(consumerClientId: String): Boolean = {
    !consumerClientId.endsWith("restore-consumer")
  }
}
