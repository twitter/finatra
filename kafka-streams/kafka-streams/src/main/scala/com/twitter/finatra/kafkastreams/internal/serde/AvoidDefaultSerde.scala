package com.twitter.finatra.kafkastreams.internal.serde

import java.util
import org.apache.kafka.common.serialization.{Deserializer, Serde, Serializer}

private[kafkastreams] class AvoidDefaultSerde extends Serde[Object] {

  private val exceptionErrorStr =
    "should be avoided as they are error prone and often result in confusing error messages. " +
      "Instead, explicitly specify your serdes. See https://kafka.apache.org/10/documentation/streams/developer-guide/datatypes.html#overriding-default-serdes"

  override def configure(configs: util.Map[String, _], isKey: Boolean): Unit = {}

  override def deserializer(): Deserializer[Object] = {
    throw new Exception(s"Default Deserializer's $exceptionErrorStr")
  }

  override def serializer(): Serializer[Object] = {
    throw new Exception(s"Default Serializer's $exceptionErrorStr")
  }

  override def close(): Unit = {}
}
