package com.twitter.finatra.kafka.serde

import java.util
import org.apache.kafka.common.serialization.{Deserializer, Serde, Serializer}

abstract class AbstractSerde[T] extends Serde[T] {

  private var _topic: String = _

  private val _deserializer = new Deserializer[T] {
    override def configure(configs: util.Map[String, _], isKey: Boolean): Unit = {}

    final override def deserialize(topic: String, bytes: Array[Byte]): T = {
      if (bytes == null) {
        null.asInstanceOf[T]
      } else {
        _topic = topic
        AbstractSerde.this.deserialize(bytes)
      }
    }

    override def close(): Unit = {}
  }

  private val _serializer = new Serializer[T] {
    override def configure(configs: util.Map[String, _], isKey: Boolean): Unit = {}

    final override def serialize(topic: String, obj: T): Array[Byte] = {
      if (obj == null) {
        null
      } else {
        _topic = topic
        AbstractSerde.this.serialize(obj)
      }
    }

    override def close(): Unit = {}
  }

  /* Public Abstract */

  def deserialize(bytes: Array[Byte]): T

  def serialize(obj: T): Array[Byte]

  /* Public */

  override def configure(configs: util.Map[String, _], isKey: Boolean): Unit = {}

  override def deserializer(): Deserializer[T] = {
    _deserializer
  }

  override def serializer(): Serializer[T] = {
    _serializer
  }

  override def close(): Unit = {}

  /**
   * The topic of the element being serialized or deserialized
   * Note: topic is only available when called from the "deserialize" or "serialize" methods
   */
  final def topic: String = _topic
}
