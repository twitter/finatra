package com.twitter.finatra.kafkastreams.transformer.aggregation

import com.twitter.finatra.kafka.serde.AbstractSerde
import java.nio.ByteBuffer
import org.apache.kafka.common.serialization.Serde

object WindowedValueSerde {
  def apply[V](inner: Serde[V]): WindowedValueSerde[V] = {
    new WindowedValueSerde[V](inner)
  }
}

/**
 * Serde for the [[WindowedValue]] class.
 *
 * @param inner Serde for [[WindowedValue.value]].
 */
class WindowedValueSerde[V](inner: Serde[V]) extends AbstractSerde[WindowedValue[V]] {

  private val innerDeserializer = inner.deserializer()
  private val innerSerializer = inner.serializer()

  override def deserialize(bytes: Array[Byte]): WindowedValue[V] = {
    val resultState = WindowResultType(bytes(0))

    val valueBytes = new Array[Byte](bytes.length - 1)
    System.arraycopy(bytes, 1, valueBytes, 0, valueBytes.length)
    val value = innerDeserializer.deserialize(topic, valueBytes)

    WindowedValue(windowResultType = resultState, value = value)
  }

  override def serialize(windowedValue: WindowedValue[V]): Array[Byte] = {
    val valueBytes = innerSerializer.serialize(topic, windowedValue.value)

    val resultTypeAndValueBytes = new Array[Byte](1 + valueBytes.size)
    val bb = ByteBuffer.wrap(resultTypeAndValueBytes)
    bb.put(windowedValue.windowResultType.value)
    bb.put(valueBytes)
    resultTypeAndValueBytes
  }
}
