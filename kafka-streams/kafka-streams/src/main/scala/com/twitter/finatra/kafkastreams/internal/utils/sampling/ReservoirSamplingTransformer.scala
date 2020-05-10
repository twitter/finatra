package com.twitter.finatra.kafkastreams.internal.utils.sampling

import com.twitter.finagle.stats.StatsReceiver
import com.twitter.finatra.kafkastreams.transformer.FinatraTransformer
import com.twitter.finatra.kafkastreams.transformer.domain.{Expire, Time, TimerMetadata}
import com.twitter.finatra.kafkastreams.transformer.stores.PersistentTimers
import com.twitter.util.Duration
import org.apache.kafka.streams.processor.PunctuationType
import scala.reflect.ClassTag
import scala.util.Random

/**
 * A Reservoir sampling transformer
 *
 * See "Random Sampling with a Reservoir", Vitter, 1985
 */
class ReservoirSamplingTransformer[
  Key: ClassTag,
  Value,
  SampleKey: ClassTag,
  SampleValue: ClassTag
](
  statsReceiver: StatsReceiver,
  toSampleKey: (Key, Value) => SampleKey,
  toSampleValue: (Key, Value) => SampleValue,
  sampleSize: Int,
  expirationTime: Option[Duration],
  countStoreName: String,
  sampleStoreName: String,
  timerStoreName: String)
    extends FinatraTransformer[Key, Value, SampleKey, SampleValue](statsReceiver = statsReceiver)
    with PersistentTimers {

  private val numExpiredCounter = statsReceiver.counter("numExpired")
  private val random = new Random()

  private val countStore = getKeyValueStore[SampleKey, Long](countStoreName)
  private val sampleStore =
    getKeyValueStore[IndexedSampleKey[SampleKey], SampleValue](sampleStoreName)
  private val timerStore =
    getPersistentTimerStore[SampleKey](timerStoreName, onEventTimer, PunctuationType.STREAM_TIME)

  override protected[finatra] def onMessage(messageTime: Time, key: Key, value: Value): Unit = {
    val sampleKey = toSampleKey(key, value)
    val totalCount = countStore.getOrDefault(sampleKey, 0)

    for (eTime <- expirationTime) {
      if (isFirstTimeSampleKeySeen(totalCount)) {
        timerStore.addTimer(messageTime + eTime, Expire, sampleKey)
      }
    }

    sample(sampleKey, toSampleValue(key, value), totalCount)
    countStore.put(sampleKey, totalCount + 1)
  }

  /* Private */

  private def onEventTimer(time: Time, metadata: TimerMetadata, key: SampleKey): Unit = {
    assert(metadata == Expire)
    countStore.delete(key)

    sampleStore
      .deleteRange(IndexedSampleKey.rangeStart(key), IndexedSampleKey.rangeEnd(key))

    numExpiredCounter.incr()
  }

  private def isFirstTimeSampleKeySeen(count: Long): Boolean = {
    // an empty value will be returned as 0
    count == 0
  }

  private def getNextSampleIndex(count: Int) = {
    if (count < sampleSize) {
      count
    } else {
      random.nextInt(count)
    }
  }

  private def sample(sampleKey: SampleKey, value: SampleValue, count: Long): Unit = {
    val sampleIndex = getNextSampleIndex(count.toInt)
    if (sampleIndex < sampleSize) {
      sampleStore.put(IndexedSampleKey(sampleKey, sampleIndex), value)
    }
  }
}
