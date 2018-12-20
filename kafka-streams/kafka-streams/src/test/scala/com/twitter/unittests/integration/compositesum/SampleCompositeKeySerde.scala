package com.twitter.unittests.integration.compositesum

import com.twitter.finatra.kafka.serde.AbstractSerde
import java.nio.ByteBuffer

object SampleCompositeKeySerde extends AbstractSerde[SampleCompositeKey] {
  override def deserialize(bytes: Array[Byte]): SampleCompositeKey = {
    val bb = ByteBuffer.wrap(bytes)
    SampleCompositeKey(bb.getInt, bb.getInt)
  }

  override def serialize(obj: SampleCompositeKey): Array[Byte] = {
    val bb = ByteBuffer.allocate(4 + 4)
    bb.putInt(obj.primary)
    bb.putInt(obj.secondary)
    bb.array()
  }
}
