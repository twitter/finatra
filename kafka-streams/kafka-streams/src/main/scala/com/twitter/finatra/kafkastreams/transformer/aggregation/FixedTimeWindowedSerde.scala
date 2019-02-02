package com.twitter.finatra.kafkastreams.transformer.aggregation

import com.twitter.finatra.kafka.serde.AbstractSerde
import com.twitter.finatra.kafkastreams.transformer.domain.Time
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

  /* Public */

  final override def deserialize(bytes: Array[Byte]): TimeWindowed[K] = {
    val keyBytesSize = bytes.length - WindowStartTimeSizeBytes
    val keyBytes = new Array[Byte](keyBytesSize)

    val bb = ByteBuffer.wrap(bytes)
    val startMs = bb.getLong()
    bb.get(keyBytes)
    val endMs = startMs + windowSizeMillis

    TimeWindowed(
      start = Time(startMs),
      end = Time(endMs),
      innerDeserializer.deserialize(topic, keyBytes)
    )
  }

  final override def serialize(timeWindowedKey: TimeWindowed[K]): Array[Byte] = {
    assert(
      timeWindowedKey.start + windowSize == timeWindowedKey.end,
      s"TimeWindowed element being serialized has end time which is not consistent with the FixedTimeWindowedSerde window size of $windowSize. ${timeWindowedKey.start + windowSize} != ${timeWindowedKey.end}"
    )

    val keyBytes = innerSerializer.serialize(topic, timeWindowedKey.value)
    val windowAndKeyBytesSize = new Array[Byte](WindowStartTimeSizeBytes + keyBytes.length)

    val bb = ByteBuffer.wrap(windowAndKeyBytesSize)
    bb.putLong(timeWindowedKey.start.millis)
    bb.put(keyBytes)
    bb.array()
  }
}
