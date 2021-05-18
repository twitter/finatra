package com.twitter.finatra.kafka.consumers

import com.twitter.conversions.DurationOps._
import com.twitter.finatra.kafka.config.{KafkaConfig, ToKafkaProperties}
import com.twitter.finatra.kafka.domain.SeekStrategy
import com.twitter.finatra.kafka.stats.KafkaFinagleMetricsReporter
import com.twitter.util.Duration
import java.util.Properties
import org.apache.kafka.clients.consumer.{ConsumerConfig, KafkaConsumer}
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

  /**
   * The KafkaConsumerConfig config, which can be used to build KafkaConsumer
   */
  def config: FinagleKafkaConsumerConfig[K, V]
  protected def fromFinagleConsumerConfig(config: FinagleKafkaConsumerConfig[K, V]): This

  override protected def configMap: Map[String, String] =
    config.kafkaConsumerConfig.configMap
  override protected def fromConfigMap(configMap: Map[String, String]): This =
    fromFinagleConsumerConfig(
      config.copy(kafkaConsumerConfig = KafkaConsumerConfig(configMap))
    )

  /**
   * Deserializer class for key
   */
  def keyDeserializer(keyDeserializer: Deserializer[K]): This =
    fromFinagleConsumerConfig(config.copy(keyDeserializer = Some(keyDeserializer)))

  /**
   * Deserializer class for value
   */
  def valueDeserializer(valueDeserializer: Deserializer[V]): This =
    fromFinagleConsumerConfig(
      config.copy(valueDeserializer = Some(valueDeserializer))
    )

  /**
   * Default poll timeout in milliseconds
   */
  def pollTimeout(pollTimeout: Duration): This =
    fromFinagleConsumerConfig(config.copy(pollTimeout = pollTimeout))

  /**
   * Whether the consumer should start from end, beginning or from the offset
   */
  def seekStrategy(seekStrategy: SeekStrategy): This =
    fromFinagleConsumerConfig(config.copy(seekStrategy = seekStrategy))

  /**
   * If using SeekStrategy.REWIND, specify the duration back in time to rewind and start consuming from
   */
  def rewindDuration(rewindDuration: Duration): This =
    fromFinagleConsumerConfig(config.copy(rewindDuration = Some(rewindDuration)))

  /**
   * For KafkaFinagleMetricsReporter: whether to include node-level metrics.
   */
  def includeNodeMetrics(include: Boolean): This =
    fromFinagleConsumerConfig(config.copy(includeNodeMetrics = include))

  /**
   * For KafkaFinagleMetricsReporter: whether to include node-level metrics.
   */
  def includePartitionMetrics(include: Boolean): This =
    fromFinagleConsumerConfig(config.copy(includePartitionMetrics = include))

  @deprecated("Use buildClient instead", "2018-05-30")
  def build(): FinagleKafkaConsumer[K, V] = {
    validateConfigs(config)
    new FinagleKafkaConsumer[K, V](config)
  }

  /**
   * Create the native KafkaConsumer client.
   */
  def buildClient(): KafkaConsumer[K, V] = {
    validateConfigs(config)
    new KafkaConsumer[K, V](
      config.properties,
      config.keyDeserializer.get,
      config.valueDeserializer.get)
  }

  protected def validateConfigs(config: FinagleKafkaConsumerConfig[K, V]): Unit = {
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
  override val config: FinagleKafkaConsumerConfig[K, V] = FinagleKafkaConsumerConfig[K, V]())
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
  includeNodeMetrics: Boolean = false,
  includePartitionMetrics: Boolean = true)
    extends KafkaConfig
    with ToKafkaProperties {
  override protected def configMap: Map[String, String] = kafkaConsumerConfig.configMap

  override def properties: Properties = {
    val properties = super.properties

    properties.put(KafkaFinagleMetricsReporter.IncludeNodeMetrics, includeNodeMetrics.toString)
    properties.put(
      KafkaFinagleMetricsReporter.IncludePartitionMetrics,
      includePartitionMetrics.toString)

    properties
  }
}
