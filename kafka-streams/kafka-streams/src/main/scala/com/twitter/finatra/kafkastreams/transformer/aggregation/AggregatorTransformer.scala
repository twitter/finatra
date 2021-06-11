package com.twitter.finatra.kafkastreams.transformer.aggregation

import com.twitter.finagle.stats.StatsReceiver
import com.twitter.finatra.kafka.serde.ScalaSerdes
import com.twitter.finatra.kafkastreams.transformer.FinatraTransformer
import com.twitter.finatra.kafkastreams.transformer.FinatraTransformer.WindowStartTime
import com.twitter.finatra.kafkastreams.transformer.domain._
import com.twitter.finatra.kafkastreams.transformer.stores.{CachingKeyValueStores, PersistentTimers}
import com.twitter.util.Duration
import it.unimi.dsi.fastutil.longs.LongOpenHashSet
import org.apache.kafka.streams.processor.PunctuationType
import org.apache.kafka.streams.state.KeyValueIterator

/**
 * An aggregating transformer for fixed windows which
 * offers additional controls that are not included in the built in Kafka Streams Windowing DSL
 *
 * A TimeWindow is a tumbling window of fixed length defined by the windowSize parameter.
 *
 * A Window is closed after event time passes the end of a TimeWindow + allowedLateness.
 *
 * After a window is closed, if emitOnClose=true it is forwarded out of this transformer with a
 * [[WindowedValue.windowResultType]] of [[WindowClosed]]
 *
 * If a record arrives after a window is closed it is immediately forwarded out of this
 * transformer with a [[WindowedValue.windowResultType]] of [[Restatement]]
 *
 * @param statsReceiver The StatsReceiver for collecting stats
 * @param stateStoreName the name of the StateStore used to maintain the counts.
 * @param timerStoreName the name of the StateStore used to maintain the timers.
 * @param windowSize splits the stream of data into buckets of data of windowSize,
 *                   based on the timestamp of each message.
 * @param allowedLateness allow messages that are up to this amount late to be added to the
 *                        store, otherwise they are emitted as restatements.
 * @param queryableAfterClose allow state to be queried up to this amount after the window is closed.
 * @param initializer Initializer function that computes an initial intermediate aggregation result
 * @param aggregator Aggregator function that computes a new aggregate result
 * @param emitOnClose Emit messages for each entry in the window when the window close. Emitted
 *                    entries will have a WindowResultType set to WindowClosed.
 * @param emitUpdatedEntriesOnCommit Emit messages for each updated entry in the window on the Kafka
 *                                   Streams commit interval. Emitted entries will have a
 *                                   WindowResultType set to WindowOpen.
 *
 * @return a stream of Keys for a particular timewindow, and the aggregations of the values for that
 *         key within a particular timewindow.
 */
class AggregatorTransformer[K, V, Aggregate](
  statsReceiver: StatsReceiver,
  stateStoreName: String,
  timerStoreName: String,
  windowSize: Duration,
  allowedLateness: Duration,
  initializer: () => Aggregate,
  aggregator: ((K, V), Aggregate) => Aggregate,
  customWindowStart: (Time, K, V) => Time,
  emitOnClose: Boolean = false,
  queryableAfterClose: Duration,
  emitUpdatedEntriesOnCommit: Boolean,
  val commitInterval: Duration)
    extends FinatraTransformer[K, V, TimeWindowed[K], WindowedValue[Aggregate]](statsReceiver)
    with CachingKeyValueStores[K, V, TimeWindowed[K], WindowedValue[Aggregate]]
    with PersistentTimers {

  private val windowSizeMillis = windowSize.inMillis
  private val allowedLatenessMillis = allowedLateness.inMillis
  private val queryableAfterCloseMillis = queryableAfterClose.inMillis

  private val emitEarlyCounter = statsReceiver.counter("emitEarly")
  private val closedWindowCounter = statsReceiver.counter("closedWindows")
  private val expiredWindowCounter = statsReceiver.counter("expiredWindows")
  private val restatementsCounter = statsReceiver.counter("numRestatements")

  private val longSerializer = ScalaSerdes.Long.serializer
  private val nonExpiredWindowStartTimes = new LongOpenHashSet()

  private val stateStore = getCachingKeyValueStore[TimeWindowed[K], Aggregate](
    name = stateStoreName,
    flushListener = onFlushedEntry)

  private val timerStore = getPersistentTimerStore[WindowStartTime](
    timerStoreName = timerStoreName,
    onTimer = onEventTimer,
    punctuationType = PunctuationType.STREAM_TIME)

  /* Public */

  override def onInit(): Unit = {
    super.onInit()
    nonExpiredWindowStartTimes.clear()
  }

  override def onMessage(time: Time, key: K, value: V): Unit = {
    val windowedKey =
      TimeWindowed.forSize(start = windowStart(time, key, value), size = windowSize, value = key)

    if (windowedKey.isLate(allowedLateness, watermark)) {
      restatement(time, key, value, windowedKey)
    } else {
      addWindowTimersIfNew(windowedKey.start.millis)

      val currentAggregateValue = stateStore.getOrDefault(windowedKey, initializer())
      stateStore.put(windowedKey, aggregator((key, value), currentAggregateValue))
    }
  }

  /* Private */

  //TODO: Optimize for when Close and Expire are at the same time e.g. TimerMetadata.CloseAndExpire
  private def addWindowTimersIfNew(windowStartTime: WindowStartTime): Unit = {
    val isNewWindow = nonExpiredWindowStartTimes.add(windowStartTime)
    if (isNewWindow) {
      val closeTime = windowStartTime + windowSizeMillis + allowedLatenessMillis
      if (emitOnClose) {
        timerStore.addTimer(Time(closeTime), Close, windowStartTime)
      }

      timerStore.addTimer(Time(closeTime + queryableAfterCloseMillis), Expire, windowStartTime)
    }
  }

  private def onFlushedEntry(
    storeName: String,
    timeWindowedKey: TimeWindowed[K],
    value: Aggregate
  ): Unit = {
    if (emitUpdatedEntriesOnCommit) {
      emitEarlyCounter.incr()
      trace(s"OnFlushedEntry: $storeName $timeWindowedKey $value")
      forward(
        key = timeWindowedKey,
        value = WindowedValue(windowResultType = WindowOpen, value = value),
        timestamp = forwardTime)
    }
  }

  private def restatement(time: Time, key: K, value: V, windowedKey: TimeWindowed[K]): Unit = {
    val windowedValue =
      WindowedValue(windowResultType = Restatement, value = aggregator((key, value), initializer()))

    forward(key = windowedKey, value = windowedValue, timestamp = forwardTime)

    restatementsCounter.incr()
  }

  private def onEventTimer(
    time: Time,
    timerMetadata: TimerMetadata,
    windowStartTime: WindowStartTime
  ): Unit = {
    debug(
      s"onEventTimer $time $timerMetadata WindowStartTime(${windowStartTime.iso8601Millis}) $watermark")
    val windowedEntriesIterator = stateStore.range(
      fromBytesInclusive = windowStartTimeBytes(windowStartTime),
      toBytesExclusive = windowStartTimeBytes(windowStartTime + 1))

    try {
      if (timerMetadata == Close) {
        onClosed(windowStartTime, windowedEntriesIterator)
      } else {
        onExpired(windowStartTime, windowedEntriesIterator)
      }
    } finally {
      windowedEntriesIterator.close()
    }
  }

  private def onClosed(
    windowStartTime: WindowStartTime,
    windowIterator: KeyValueIterator[TimeWindowed[K], Aggregate]
  ): Unit = {
    while (windowIterator.hasNext) {
      val entry = windowIterator.next()
      assert(entry.key.start.millis == windowStartTime)
      forward(
        key = entry.key,
        value = WindowedValue(windowResultType = WindowClosed, value = entry.value),
        timestamp = forwardTime)
    }

    closedWindowCounter.incr()
  }

  private def onExpired(
    windowStartTime: WindowStartTime,
    windowIterator: KeyValueIterator[TimeWindowed[K], Aggregate]
  ): Unit = {
    stateStore.deleteRangeExperimentalWithNoChangelogUpdates(
      beginKeyInclusive = windowStartTimeBytes(windowStartTime),
      endKeyExclusive = windowStartTimeBytes(windowStartTime + 1))

    nonExpiredWindowStartTimes.remove(windowStartTime)

    expiredWindowCounter.incr()
  }

  private def windowStartTimeBytes(windowStartMs: Long): Array[Byte] = {
    longSerializer.serialize("", windowStartMs)
  }

  private def windowStart(time: Time, key: K, value: V): Time = {
    if (customWindowStart != null) {
      customWindowStart(time, key, value)
    } else {
      TimeWindowed.windowStart(time, windowSize)
    }
  }

  private def forwardTime: Long = {
    watermark.timeMillis
  }
}
