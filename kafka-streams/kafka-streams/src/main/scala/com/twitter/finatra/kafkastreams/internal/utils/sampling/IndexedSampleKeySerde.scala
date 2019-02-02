package com.twitter.finatra.kafkastreams.internal.utils.sampling

import com.google.common.primitives.Ints
import com.twitter.finatra.kafka.serde.AbstractSerde
import java.nio.ByteBuffer
import org.apache.kafka.common.serialization.Serde

private[kafkastreams] object IndexedSampleKeySerde {

  /**
   * Indexed sample key adds one Integer to the bytes
   */
  val IndexSize: Int = Ints.BYTES
}

private[kafkastreams] class IndexedSampleKeySerde[SampleKey](sampleKeySerde: Serde[SampleKey])
    extends AbstractSerde[IndexedSampleKey[SampleKey]] {

  private val sampleKeySerializer = sampleKeySerde.serializer()
  private val sampleKeyDeserializer = sampleKeySerde.deserializer()

  override def deserialize(bytes: Array[Byte]): IndexedSampleKey[SampleKey] = {
    val bb = ByteBuffer.wrap(bytes)

    val sampleKeyBytesLength = bytes.length - IndexedSampleKeySerde.IndexSize
    val sampleKeyBytes = new Array[Byte](sampleKeyBytesLength)
    bb.get(sampleKeyBytes)
    val sampleKey = sampleKeyDeserializer.deserialize(topic, sampleKeyBytes)

    val index = bb.getInt()

    IndexedSampleKey(sampleKey, index)
  }

  override def serialize(indexedSampleKey: IndexedSampleKey[SampleKey]): Array[Byte] = {
    val sampleKeyBytes = sampleKeySerializer.serialize(topic, indexedSampleKey.sampleKey)
    val sampleKeyBytesLength = sampleKeyBytes.length

    val indexedSampleKeyBytes =
      new Array[Byte](sampleKeyBytesLength + IndexedSampleKeySerde.IndexSize)
    val bb = ByteBuffer.wrap(indexedSampleKeyBytes)
    bb.put(sampleKeyBytes)

    bb.putInt(indexedSampleKey.index)

    indexedSampleKeyBytes
  }
}
