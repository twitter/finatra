package com.twitter.finatra.kafka.serde

import java.util
import org.apache.kafka.common.serialization.{Deserializer, Serde, Serializer}

object UnKeyedSerde extends Serde[UnKeyed] {
  override def configure(configs: util.Map[String, _], isKey: Boolean): Unit = {}

  override def deserializer: Deserializer[UnKeyed] = new Deserializer[UnKeyed] {
    override def configure(configs: util.Map[String, _], isKey: Boolean): Unit = {}

    override def close(): Unit = {}

    override def deserialize(topic: String, data: Array[Byte]): UnKeyed = {
      UnKeyed
    }
  }

  override def serializer: Serializer[UnKeyed] = new Serializer[UnKeyed] {
    override def configure(configs: util.Map[String, _], isKey: Boolean): Unit = {}

    override def serialize(topic: String, data: UnKeyed): Array[Byte] = {
      null
    }

    override def close(): Unit = {}
  }

  override def close(): Unit = {}
}
