package com.twitter.finatra.kafkastreams.internal.utils

import com.twitter.app.{App, Flag, Flaggable}
import org.apache.kafka.streams.StreamsConfig

private[kafkastreams] trait KafkaFlagUtils extends App {

  def requiredKafkaFlag[T: Flaggable: Manifest](key: String, helpPrefix: String = ""): Flag[T] = {
    flag[T](name = "kafka." + key, help = helpPrefix + kafkaDocumentation(key))
  }

  def flagWithKafkaDefault[T: Flaggable](key: String): Flag[T] = {
    kafkaFlag[T](key, getKafkaDefault[T](key))
  }

  def kafkaFlag[T: Flaggable](key: String, default: => T): Flag[T] = {
    kafkaFlag[T](key, default, kafkaDocumentation(key))
  }

  def kafkaFlag[T: Flaggable](key: String, default: => T, helpDoc: String): Flag[T] = {
    flag(
      name = "kafka." + key,
      default = default,
      help = helpDoc
    )
  }

  def kafkaDocumentation(key: String): String = {
    val configKey = StreamsConfig.configDef.configKeys.get(key)
    if (configKey == null) {
      throw new Exception("Kafka Config Key Not Found: " + key)
    }
    configKey.documentation
  }

  def getKafkaDefault[T](key: String): T = {
    val configKey = StreamsConfig.configDef().configKeys().get(key)
    if (configKey == null) {
      throw new Exception("Kafka Config Key Not Found: " + key)
    } else if (!configKey.hasDefault) {
      throw new Exception(
        s"Kafka doesn't have a default value for ${key}, please provide a default value"
      )
    }
    configKey.defaultValue.asInstanceOf[T]
  }
}
