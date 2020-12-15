package com.twitter.finatra.kafka.config

import com.twitter.util.{Duration, StorageUnit}
import java.util.Properties

/**
 * Base trait for everything Kafka config related.
 * Kafka's configuration eventually ends up in a
 * java.util.Properties (see ToKafkaProperties below).
 *
 * We keep it in a Map[String, String] for convenience
 * until the last possible moment.
 */
trait KafkaConfig {
  protected def configMap: Map[String, String]
}

/**
 * Base trait for making builders that set kafka config. Gives you helpers
 * for setting the config with values of different types:
 *   - Time
 *   - StorageUnit
 *   - class name
 *
 * If your builder would be useful as part of another builder,
 * implement your methods in a method trait that extends KafkaConfigMethods,
 * so that other builders can include you.
 * See KafkaProducerConfigMethods and FinagleKafkaConsumerBuilderMethods
 * for examples of this pattern.
 *
 * @tparam Self The type of your concrete builder. This lets all the convenience
 *              methods here and all the methods defined in intermediate traits
 *              return that type.
 */
trait KafkaConfigMethods[Self] extends KafkaConfig {
  type This = Self

  /**
   *  Override this in your concrete builder with a copy constructor for that
   *  builder that replaces the old configMap with a modified one.
   */
  protected def fromConfigMap(configMap: Map[String, String]): This

  def withConfig(key: String, value: String): This =
    fromConfigMap(configMap + (key -> value))

  def withConfig(key: String, value: Int): This =
    fromConfigMap(configMap + (key -> value.toString))

  def withConfig(key: String, value: Boolean): This =
    fromConfigMap(configMap + (key -> value.toString))

  def withConfig(key: String, value: Duration): This = {
    fromConfigMap(configMap + (key -> value.inMilliseconds.toString))
  }

  def withConfig(key: String, value: StorageUnit): This = {
    fromConfigMap(configMap + (key -> value.bytes.toString))
  }

  def withConfig(keyValuesMap: Map[String, String]): This = {
    fromConfigMap(configMap ++ keyValuesMap)
  }

  protected def withClassName[T: Manifest](key: String): This = {
    fromConfigMap(configMap + (key -> manifest[T].runtimeClass.getName))
  }

  protected def withClassNameBuilder[T: Manifest](key: String): This = {
    val className = manifest[T].runtimeClass.getName
    val classes = configMap.get(key) match {
      case Some(classNameValues) => s"$classNameValues,$className"
      case _ => className
    }
    fromConfigMap(configMap + (key -> classes))
  }
}

/**
 * Extend in your concrete configuration object so that the configMap
 * can be converted to java.util.Properties.
 *
 * See KafkaProducerConfig and FinagleKafkaConsumerConfig
 * for examples of this pattern.
 */
trait ToKafkaProperties { self: KafkaConfig =>
  def properties: Properties = {
    val p = new Properties
    configMap.foreach {
      case (k, v) =>
        // allow null property, and ignore the null properties
        if (v != null)
          p.setProperty(k, v)
    }
    p
  }
}
