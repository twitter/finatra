package com.twitter.finatra.kafka.producers

import com.twitter.finagle.stats.{LoadedStatsReceiver, StatsReceiver}
import com.twitter.finatra.kafka.config.{KafkaConfig, ToKafkaProperties}
import com.twitter.finatra.kafka.stats.KafkaFinagleMetricsReporter
import java.util.Properties
import org.apache.kafka.clients.producer.{KafkaProducer, ProducerConfig}
import org.apache.kafka.common.serialization.Serializer

case class FinagleKafkaProducerBuilder[K, V](
  config: FinagleKafkaProducerConfig[K, V] = FinagleKafkaProducerConfig[K, V]())
    extends KafkaProducerConfigMethods[FinagleKafkaProducerBuilder[K, V]] {
  override protected def fromConfigMap(configMap: Map[String, String]): This =
    FinagleKafkaProducerBuilder(config.copy(kafkaProducerConfig = KafkaProducerConfig(configMap)))

  override protected def configMap: Map[String, String] = config.kafkaProducerConfig.configMap

  protected def withConfig(config: FinagleKafkaProducerConfig[K, V]): This =
    new FinagleKafkaProducerBuilder[K, V](config)

  /**
   * Serializer class for key
   */
  def keySerializer(keySerializer: Serializer[K]): This =
    withConfig(config.copy(keySerializer = Some(keySerializer)))

  /**
   * Serializer class for value
   */
  def valueSerializer(valueSerializer: Serializer[V]): This =
    withConfig(config.copy(valueSerializer = Some(valueSerializer)))

  /**
   * For KafkaFinagleMetricsReporter: whether to include node-level metrics.
   */
  def includeNodeMetrics(include: Boolean): This =
    withConfig(config.copy(includeNodeMetrics = include))

  def statsReceiver(statsReceiver: StatsReceiver): This =
    withConfig(config.copy(statsReceiver = statsReceiver))

  def build(): FinagleKafkaProducer[K, V] = {
    validateConfigs(config)
    new FinagleKafkaProducer[K, V](config)
  }

  /**
   * Create the native KafkaProducer client.
   */
  def buildClient(): KafkaProducer[K, V] = {
    validateConfigs(config)
    TracingKafkaProducer[K, V](
      config.properties,
      config.keySerializer.get,
      config.valueSerializer.get)
  }

  private def validateConfigs(config: FinagleKafkaProducerConfig[K, V]): Unit = {
    require(
      configMap.get(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG).isDefined,
      "FinagleKafkaProducerBuilder: dest must be configured"
    )
    require(
      config.keySerializer.isDefined,
      "FinagleKafkaProducerBuilder: keySerializer must be configured"
    )
    require(
      config.valueSerializer.isDefined,
      "FinagleKafkaProducerBuilder: valueSerializer must be configured"
    )
    require(
      configMap.get(ProducerConfig.CLIENT_ID_CONFIG).isDefined,
      "FinagleKafkaProducerBuilder: clientId must be configured"
    )
  }
}

case class FinagleKafkaProducerConfig[K, V](
  kafkaProducerConfig: KafkaProducerConfig = KafkaProducerConfig(),
  keySerializer: Option[Serializer[K]] = None,
  valueSerializer: Option[Serializer[V]] = None,
  includeNodeMetrics: Boolean = false,
  statsReceiver: StatsReceiver = LoadedStatsReceiver)
    extends KafkaConfig
    with ToKafkaProperties {
  override def configMap: Map[String, String] = kafkaProducerConfig.configMap
  override def properties: Properties = {
    val properties = super.properties

    properties.put(KafkaFinagleMetricsReporter.IncludeNodeMetrics, includeNodeMetrics.toString)

    properties
  }
}
