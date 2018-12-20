package com.twitter.finatra.streams.transformer.domain

import com.twitter.finatra.kafka.serde.AbstractSerde
import com.twitter.util.Duration
import java.nio.ByteBuffer
import org.apache.kafka.common.serialization.Serde

object FixedTimeWindowedSerde {

  def apply[K](inner: Serde[K], duration: Duration): Serde[TimeWindowed[K]] = {
    new FixedTimeWindowedSerde[K](inner, duration)
  }
}

/**
 * Serde for use when your time windows are fixed length and non-overlapping. When this
 * condition is met, we are able to avoid serializing 8 bytes for TimeWindowed.endTime
 */
class FixedTimeWindowedSerde[K](val inner: Serde[K], windowSize: Duration)
    extends AbstractSerde[TimeWindowed[K]] {

  private val WindowStartTimeSizeBytes = java.lang.Long.BYTES

  private val innerDeserializer = inner.deserializer()
  private val innerSerializer = inner.serializer()
  private val windowSizeMillis = windowSize.inMillis
  assert(windowSizeMillis > 10, "The minimum window size currently supported is 10ms")

  final override def deserialize(bytes: Array[Byte]): TimeWindowed[K] = {
    val keyBytesSize = bytes.length - WindowStartTimeSizeBytes
    val keyBytes = new Array[Byte](keyBytesSize)

    val bb = ByteBuffer.wrap(bytes)
    val startMs = bb.getLong()
    val endMs = startMs + windowSizeMillis
    bb.get(keyBytes)

    TimeWindowed(startMs = startMs, endMs = endMs, innerDeserializer.deserialize(topic, keyBytes))
  }

  final override def serialize(timeWindowedKey: TimeWindowed[K]): Array[Byte] = {
    assert(
      timeWindowedKey.endMs == Long.MaxValue || timeWindowedKey.startMs + windowSizeMillis == timeWindowedKey.endMs,
      s"TimeWindowed element being serialized has end time which is not consistent with the FixedTimeWindowedSerde window size of $windowSize. ${timeWindowedKey.startMs + windowSizeMillis} != ${timeWindowedKey.endMs}"
    )

    val keyBytes = innerSerializer.serialize(topic, timeWindowedKey.value)
    val windowAndKeyBytesSize = new Array[Byte](WindowStartTimeSizeBytes + keyBytes.length)

    val bb = ByteBuffer.wrap(windowAndKeyBytesSize)
    bb.putLong(startMs(timeWindowedKey))
    bb.put(keyBytes)
    bb.array()
  }

  /*
   * We need to special case fixed fixed windows when the endMs is Long.MaxValue which signals all elements in this window
   * We special case because this fixed window serde doesn't serialize endMs to save bytes
   * Note: We use this value in keyValueStore.range which treats the to and from times as both inclusive (which differs from TimeWindowed which is documented as from being exclusive...)
   */
  private def startMs(timeWindowedKey: TimeWindowed[K]) = {
    if (timeWindowedKey.endMs == Long.MaxValue) {
      timeWindowedKey.startMs + 1
    } else {
      timeWindowedKey.startMs
    }
  }
}
