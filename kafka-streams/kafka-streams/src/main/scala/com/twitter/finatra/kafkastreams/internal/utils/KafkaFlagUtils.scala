package com.twitter.finatra.kafkastreams.internal.utils

import com.twitter.app.{App, Flag, Flaggable}
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.config.ConfigDef
import org.apache.kafka.streams.StreamsConfig

/**
 * Utility class that can create flags for a Finatra Kafka Streams service
 * By integrating with the Kafka StreamsConfig, ProducerConfig, and ConsumerConfig classes,
 * we can ensure that our flags have the same documentation and default values as specified
 * by Kafka.
 */
private[kafkastreams] trait KafkaFlagUtils extends App {

  private val streamsConfigDef = StreamsConfig.configDef()

  private val producerConfigDef = ReflectionUtils
    .getStaticFinalField[ProducerConfig, ConfigDef]("CONFIG")

  private val consumerConfigDef = ReflectionUtils
    .getStaticFinalField[ConsumerConfig, ConfigDef]("CONFIG")

  /* Public */

  /**
   * Create a required flag prefixed by "kafka." with a help string this is populated from the
   * Kafka Streams StreamsConfig class
   *
   * @param key Name of the KafkaStreams flag as specified in the StreamsConfig class
   * @param helpPrefix A string to be added to the front of the Kafka documentation for this key
   * @tparam T Type of the flag value
   *
   * @return Flag for the specified key
   */
  def requiredKafkaFlag[T: Flaggable: Manifest](
    key: String,
    helpPrefix: String = ""
  ): Flag[T] = {
    flag[T](name = "kafka." + key, help = helpPrefix + kafkaDocumentation(streamsConfigDef, key))
  }

  /**
   * Create a flag prefixed by "kafka." with the specified default value and a help string that is
   * populated from the Kafka Streams StreamsConfig class
   *
   * @param key Name of the KafkaStreams flag as specified in the StreamsConfig class
   * @param default Default value for this flag
   * @tparam T Type of the flag value
   * @return Flag for the specified key
   */
  def kafkaFlag[T: Flaggable](key: String, default: => T): Flag[T] = {
    kafkaFlag(
      prefix = "kafka.",
      key = key,
      default = default,
      helpDoc = kafkaDocumentation(streamsConfigDef, key))
  }

  /**
   * Create a flag prefixed by "kafka." with a default value and help string that is
   * populated from the Kafka Streams StreamsConfig class
   *
   * @param key Name of the KafkaStreams flag as specified in the StreamsConfig class
   * @tparam T Type of the flag value
   * @return Flag for the specified key
   */
  def flagWithKafkaDefault[T: Flaggable](key: String): Flag[T] = {
    kafkaFlag[T](key, getKafkaDefault[T](streamsConfigDef, key))
  }

  /**
   * Create a flag prefixed by "kafka.consumer." with a default value and help string that is
   * populated from the Kafka ConsumerConfig class
   *
   * @param key Name of the KafkaStreams flag as specified in the ConsumerConfig class
   * @tparam T Type of the flag value
   * @return Flag for the specified key
   */
  def consumerFlagWithKafkaDefault[T: Flaggable](key: String): Flag[T] = {
    kafkaFlag(
      prefix = "kafka.consumer.",
      key = key,
      default = getKafkaDefault[T](consumerConfigDef, key),
      helpDoc = kafkaDocumentation(consumerConfigDef, key))
  }

  /**
   * Create a flag prefixed by "kafka.producer." with the specified default value and help string
   * that is populated from the Kafka ProducerConfig class
   *
   * @param key Name of the KafkaStreams flag as specified in the ProducerConfig class
   * @param default Default value for this flag
   * @tparam T Type of the flag value
   * @return Flag for the specified key
   */
  def producerFlag[T: Flaggable](key: String, default: => T): Flag[T] = {
    kafkaFlag(
      prefix = "kafka.producer.",
      key = key,
      default = default,
      helpDoc = kafkaDocumentation(producerConfigDef, key))
  }

  /**
   * Create a flag prefixed by "kafka.producer." with a default value and help string that is
   * populated from the Kafka ProducerConfig class
   *
   * @param key Name of the KafkaStreams flag as specified in the ProducerConfig class
   * @tparam T Type of the flag value
   * @return Flag for the specified key
   */
  def producerFlagWithKafkaDefault[T: Flaggable](key: String): Flag[T] = {
    producerFlag(key = key, default = getKafkaDefault[T](producerConfigDef, key))
  }

  /* Private */

  private def kafkaFlag[T: Flaggable](
    prefix: String,
    key: String,
    default: => T,
    helpDoc: String
  ): Flag[T] = {
    flag(
      name = prefix + key,
      default = default,
      help = helpDoc
    )
  }

  private def kafkaDocumentation(configDef: ConfigDef, key: String): String = {
    val configKey = configDef.configKeys.get(key)
    if (configKey == null) {
      throw new Exception(s"Kafka Config Key Not Found: $key in $configDef ${configDef.configKeys}")
    }
    configKey.documentation
  }

  private def getKafkaDefault[T](configDef: ConfigDef, key: String): T = {
    val configKey = configDef.configKeys().get(key)
    if (configKey == null) {
      throw new Exception("Kafka Config Key Not Found: " + key)
    } else if (!configKey.hasDefault) {
      throw new Exception(
        s"Kafka doesn't have a default value for $key, please provide a default value"
      )
    }

    if (configKey.defaultValue.isInstanceOf[Integer] && key == ProducerConfig.LINGER_MS_CONFIG) {
      // compatibility workaround: kafka 2.2 Linger is int, 2.5 is long
      configKey.defaultValue.asInstanceOf[Integer].toLong.asInstanceOf[T]
    } else {
      configKey.defaultValue.asInstanceOf[T]
    }
  }
}
