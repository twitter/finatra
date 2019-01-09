package com.twitter.finatra.kafka.consumers

import com.twitter.conversions.DurationOps._
import com.twitter.finatra.kafka.config.{KafkaConfig, ToKafkaProperties}
import com.twitter.finatra.kafka.domain.SeekStrategy
import com.twitter.finatra.kafka.stats.KafkaFinagleMetricsReporter
import com.twitter.util.Duration
import java.util.Properties
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.serialization.Deserializer

/**
 * Trait defining methods for configuring a FinagleKafkaConsumer.
 * It extends KafkaConsumerConfig, to get all the Kafka-specific methods that
 * set elements of the java.util.Properties object.
 * It also adds methods for configuring new parameters that are specific to
 * FinagleKafkaConsumer (pollTimeout and seekStrategy).
 *
 * @tparam K The key type of the consumer this will build.
 * @tparam V The value type of the consumer this will build.
 * @tparam Self The type of the concrete builder that includes these methods.
 */
trait FinagleKafkaConsumerBuilderMethods[K, V, Self] extends KafkaConsumerConfigMethods[Self] {
  protected def finagleConsumerConfig: FinagleKafkaConsumerConfig[K, V]
  protected def fromFinagleConsumerConfig(config: FinagleKafkaConsumerConfig[K, V]): This

  override protected def configMap: Map[String, String] =
    finagleConsumerConfig.kafkaConsumerConfig.configMap
  override protected def fromConfigMap(configMap: Map[String, String]): This =
    fromFinagleConsumerConfig(
      finagleConsumerConfig.copy(kafkaConsumerConfig = KafkaConsumerConfig(configMap))
    )

  /**
   * Deserializer class for key
   */
  def keyDeserializer(keyDeserializer: Deserializer[K]): This =
    fromFinagleConsumerConfig(finagleConsumerConfig.copy(keyDeserializer = Some(keyDeserializer)))

  /**
   * Deserializer class for value
   */
  def valueDeserializer(valueDeserializer: Deserializer[V]): This =
    fromFinagleConsumerConfig(
      finagleConsumerConfig.copy(valueDeserializer = Some(valueDeserializer))
    )

  /**
   * Default poll timeout in milliseconds
   */
  def pollTimeout(pollTimeout: Duration): This =
    fromFinagleConsumerConfig(finagleConsumerConfig.copy(pollTimeout = pollTimeout))

  /**
   * Whether the consumer should start from end, beginning or from the offset
   */
  def seekStrategy(seekStrategy: SeekStrategy): This =
    fromFinagleConsumerConfig(finagleConsumerConfig.copy(seekStrategy = seekStrategy))

  /**
   * If using SeekStrategy.REWIND, specify the duration back in time to rewind and start consuming from
   */
  def rewindDuration(rewindDuration: Duration): This =
    fromFinagleConsumerConfig(finagleConsumerConfig.copy(rewindDuration = Some(rewindDuration)))

  /**
   * For KafkaFinagleMetricsReporter: whether to include node-level metrics.
   */
  def includeNodeMetrics(include: Boolean): This =
    fromFinagleConsumerConfig(finagleConsumerConfig.copy(includeNodeMetrics = include))

  def build(): FinagleKafkaConsumer[K, V] = {
    validateConfigs(finagleConsumerConfig)
    new FinagleKafkaConsumer[K, V](finagleConsumerConfig)
  }

  protected def validateConfigs(config: FinagleKafkaConsumerConfig[K, V]) = {
    require(
      configMap.get(ConsumerConfig.GROUP_ID_CONFIG).isDefined,
      "FinagleKafkaConsumerBuilder: groupId must be configured"
    )
    require(
      config.keyDeserializer.isDefined,
      "FinagleKafkaConsumerBuilder: keyDeserializer must be configured"
    )
    require(
      config.valueDeserializer.isDefined,
      "FinagleKafkaConsumerBuilder: valueDeserializer must be configured"
    )
  }
}

case class FinagleKafkaConsumerBuilder[K, V](
  override protected val finagleConsumerConfig: FinagleKafkaConsumerConfig[K, V] =
    FinagleKafkaConsumerConfig[K, V]())
    extends FinagleKafkaConsumerBuilderMethods[K, V, FinagleKafkaConsumerBuilder[K, V]] {
  override protected def fromFinagleConsumerConfig(config: FinagleKafkaConsumerConfig[K, V]): This =
    new FinagleKafkaConsumerBuilder[K, V](config)
}

case class FinagleKafkaConsumerConfig[K, V](
  kafkaConsumerConfig: KafkaConsumerConfig = KafkaConsumerConfig(),
  keyDeserializer: Option[Deserializer[K]] = None,
  valueDeserializer: Option[Deserializer[V]] = None,
  pollTimeout: Duration = 100.millis,
  seekStrategy: SeekStrategy = SeekStrategy.RESUME,
  rewindDuration: Option[Duration] = None,
  includeNodeMetrics: Boolean = false)
    extends KafkaConfig
    with ToKafkaProperties {
  override protected def configMap: Map[String, String] = kafkaConsumerConfig.configMap

  override def properties: Properties = {
    val properties = super.properties

    properties.put(KafkaFinagleMetricsReporter.IncludeNodeMetrics, includeNodeMetrics.toString)

    properties
  }
}
