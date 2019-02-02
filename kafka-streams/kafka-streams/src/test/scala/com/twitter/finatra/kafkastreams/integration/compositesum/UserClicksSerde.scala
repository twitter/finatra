package com.twitter.finatra.kafkastreams.integration.compositesum

import com.google.common.primitives.Ints
import com.twitter.finatra.kafka.serde.AbstractSerde
import java.nio.ByteBuffer

object UserClicksSerde extends AbstractSerde[UserClicks] {
  override def deserialize(bytes: Array[Byte]): UserClicks = {
    val bb = ByteBuffer.wrap(bytes)
    val userId = bb.getInt()
    val clicks = bb.getInt()
    UserClicks(userId = userId, clickType = clicks)
  }

  override def serialize(obj: UserClicks): Array[Byte] = {
    val bb = ByteBuffer.allocate(Ints.BYTES + Ints.BYTES)
    bb.putInt(obj.userId)
    bb.putInt(obj.clickType)
    bb.array()
  }
}
