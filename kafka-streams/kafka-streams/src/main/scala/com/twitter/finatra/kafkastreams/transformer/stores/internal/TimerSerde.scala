package com.twitter.finatra.streams.transformer.internal.domain

import com.google.common.primitives.Longs
import com.twitter.finatra.kafka.serde.AbstractSerde
import com.twitter.finatra.kafkastreams.transformer.domain.{Time, TimerMetadata}
import com.twitter.finatra.kafkastreams.transformer.stores.internal.Timer
import java.nio.ByteBuffer
import org.apache.kafka.common.serialization.Serde

object TimerSerde {
  def apply[K](inner: Serde[K]): TimerSerde[K] = {
    new TimerSerde(inner)
  }

  def timerTimeToBytes(time: Long): Array[Byte] = {
    ByteBuffer
      .allocate(Longs.BYTES)
      .putLong(time)
      .array()
  }
}

/**
 * Serde for the [[Timer]] class.
 *
 * @param inner Serde for [[Timer.key]].
 */
class TimerSerde[K](inner: Serde[K]) extends AbstractSerde[Timer[K]] {

  private val TimerTimeSizeBytes = Longs.BYTES
  private val MetadataSizeBytes = 1

  private val innerDeser = inner.deserializer()
  private val innerSer = inner.serializer()

  final override def deserialize(bytes: Array[Byte]): Timer[K] = {
    val bb = ByteBuffer.wrap(bytes)
    val time = Time(bb.getLong())
    val metadata = TimerMetadata(bb.get)

    val keyBytes = new Array[Byte](bb.remaining())
    bb.get(keyBytes)
    val key = innerDeser.deserialize(topic, keyBytes)

    Timer(time = time, metadata = metadata, key = key)
  }

  final override def serialize(timer: Timer[K]): Array[Byte] = {
    val keyBytes = innerSer.serialize(topic, timer.key)
    val timerBytes = new Array[Byte](TimerTimeSizeBytes + MetadataSizeBytes + keyBytes.length)

    val bb = ByteBuffer.wrap(timerBytes)
    bb.putLong(timer.time.millis)
    bb.put(timer.metadata.value)
    bb.put(keyBytes)
    timerBytes
  }
}
