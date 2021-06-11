package com.twitter.finatra.kafkastreams.dsl

import com.twitter.conversions.DurationOps._
import com.twitter.finagle.stats.StatsReceiver
import com.twitter.finatra.kafka.domain.KafkaTopic
import com.twitter.finatra.kafka.producers.{
  FinagleKafkaProducerBuilder,
  FinagleKafkaProducerConfig,
  KafkaProducerConfig
}
import com.twitter.finatra.kafkastreams.flushing.FlushingAwareServer
import com.twitter.util.Duration
import org.apache.kafka.streams.StreamsConfig
import org.apache.kafka.streams.kstream.internals.ProducedInternal
import org.apache.kafka.streams.scala.kstream.{KStream, Produced}

trait FinatraDslToCluster extends FlushingAwareServer {
  implicit class ToClusterKeyValueStream[K, V](kstream: KStream[K, V]) {

    /**
     * Publishes events to a kafka topic on the specified cluster rather than the primary cluster.
     * This call is needed because KafkaStreams does not natively provide the ability
     * to publish to a cluster different from the cluster from which it is consuming.
     * Can only be used in at_least_once processing guarantee. Will not work with exactly_once.
     *
     * @param cluster               Cluster to which events are published (wily path or host:port)
     * @param topic                 Topic to which events are published
     * @param clientId              ClientId used in the FinagleKafkaProducer
     * @param statsReceiver         StatsReceiver used both in the FinagleKafkaProducer and AsyncProcessor
     * @param kafkaProducerConfig   Configuration used to configure the FinagleKafkaProducer
     * @param commitInterval        Interval at which the AsyncProcessor performs commits
     * @param flushTimeout          Timeout for which the AsyncProcessor waits for publishes to complete
     * @param maxPendingEvents Maximum number of events pending publishing
     * @param produced              An [org.apache.kafka.streams.scala.Produced] to specify the serializers for the key & value
     */
    def toCluster(
      cluster: String,
      topic: KafkaTopic,
      clientId: String,
      statsReceiver: StatsReceiver,
      kafkaProducerConfig: FinagleKafkaProducerConfig[K, V] = defaultFinagleKafkaProducerConfig,
      commitInterval: Duration = 1.minute,
      flushTimeout: Duration = Duration.Top,
      maxPendingEvents: Int = 1000
    )(
      implicit produced: Produced[K, V]
    ): Unit = {
      require(
        !kafkaProducerConfig.configMap
          .get(StreamsConfig.PROCESSING_GUARANTEE_CONFIG).contains("exactly_once"),
        "'exactly_once' processing does not work with toCluster"
      )

      val producedInternal = new ProducedInternal(produced)

      val producer = new FinagleKafkaProducerBuilder[K, V](kafkaProducerConfig)
        .dest(cluster)
        .clientId(clientId)
        .statsReceiver(statsReceiver)
        .keySerializer(producedInternal.keySerde.serializer)
        .valueSerializer(producedInternal.valueSerde.serializer)
        .build()

      kstream.process(() =>
        new KafkaProducerProcessor(
          topic = topic.name,
          statsReceiver = kafkaProducerConfig.statsReceiver.scope(s"kafka/$clientId"),
          producer = producer,
          maxOutstandingFutures = maxPendingEvents,
          commitInterval = commitInterval,
          flushTimeout = flushTimeout
        ))
    }
  }

  private def defaultFinagleKafkaProducerConfig[K, V] = FinagleKafkaProducerConfig[K, V](
    kafkaProducerConfig = KafkaProducerConfig()
      .requestTimeout(1.minute))
}
