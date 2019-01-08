package com.twitter.finatra.streams.transformer

import com.google.common.annotations.Beta
import com.twitter.conversions.DurationOps._
import com.twitter.finagle.stats.{LoadedStatsReceiver, StatsReceiver}
import com.twitter.finatra.kafkastreams.internal.utils.ProcessorContextLogging
import com.twitter.finatra.streams.config.DefaultTopicConfig
import com.twitter.finatra.streams.stores.FinatraKeyValueStore
import com.twitter.finatra.streams.stores.internal.FinatraStoresGlobalManager
import com.twitter.finatra.streams.transformer.FinatraTransformer.TimerTime
import com.twitter.finatra.streams.transformer.domain.{
  DeleteTimer,
  RetainTimer,
  Time,
  TimerMetadata,
  TimerResult
}
import com.twitter.finatra.streams.transformer.internal.domain.{Timer, TimerSerde}
import com.twitter.finatra.streams.transformer.internal.{
  OnClose,
  OnInit,
  ProcessorContextUtils,
  StateStoreImplicits,
  WatermarkTracker
}
import com.twitter.util.Duration
import org.agrona.collections.ObjectHashSet
import org.apache.kafka.common.serialization.{Serde, Serdes}
import org.apache.kafka.streams.kstream.Transformer
import org.apache.kafka.streams.processor.{
  Cancellable,
  ProcessorContext,
  PunctuationType,
  Punctuator
}
import org.apache.kafka.streams.state.{KeyValueIterator, KeyValueStore, StoreBuilder, Stores}
import org.joda.time.DateTime
import scala.collection.JavaConverters._
import scala.reflect.ClassTag

object FinatraTransformer {
  type TimerTime = Long
  type WindowStartTime = Long
  type DateTimeMillis = Long

  def timerStore[TimerKey](
    name: String,
    timerKeySerde: Serde[TimerKey]
  ): StoreBuilder[KeyValueStore[Timer[TimerKey], Array[Byte]]] = {
    Stores
      .keyValueStoreBuilder(
        Stores.persistentKeyValueStore(name),
        TimerSerde(timerKeySerde),
        Serdes.ByteArray
      )
      .withLoggingEnabled(DefaultTopicConfig.FinatraChangelogConfig)
  }
}

/**
 * A KafkaStreams Transformer supporting Per-Key Persistent Timers
 * Inspired by Flink's ProcessFunction: https://ci.apache.org/projects/flink/flink-docs-stable/dev/stream/operators/process_function.html
 *
 * Note: Timers are based on a sorted RocksDB KeyValueStore
 * Note: Timers that fire at the same time MAY NOT fire in the order which they were added
 *
 * Example Timer Key Structures (w/ corresponding CountsStore Key Structures)
 * {{{
 * ImpressionsCounter (w/ TimerKey storing TweetId)
 * TimeWindowedKey(2018-08-04T10:00:00.000Z-20)
 * Timer(          2018-08-04T12:00:00.000Z-Expire-2018-08-04T10:00:00.000Z-20
 * TimeWindowedKey(2018-08-04T10:00:00.000Z-30)
 * Timer(          2018-08-04T12:00:00.000Z-Expire-2018-08-04T10:00:00.000Z-30
 *
 * ImpressionsCounter (w/ TimerKey storing windowStartMs)
 * TimeWindowedKey(2018-08-04T10:00:00.000Z-20)
 * TimeWindowedKey(2018-08-04T10:00:00.000Z-30)
 * TimeWindowedKey(2018-08-04T10:00:00.000Z-40)
 * TimeWindowedKey(2018-08-04T11:00:00.000Z-20)
 * TimeWindowedKey(2018-08-04T11:00:00.000Z-30)
 * Timer(          2018-08-04T12:00:00.000Z-Expire-2018-08-04T10:00:00.000Z
 * Timer(          2018-08-04T13:00:00.000Z-Expire-2018-08-04T11:00:00.000Z
 *
 * EngagementCounter (w/ TimerKey storing windowStartMs)
 * TimeWindowedKey(2018-08-04T10:00:00.000Z-20-displayed) -> 5
 * TimeWindowedKey(2018-08-04T10:00:00.000Z-20-fav) -> 10
 * Timer(          2018-08-04T12:00:00.000Z-Expire-2018-08-04T10:00:00.000Z
 *
 * @tparam InputKey    Type of the input keys
 * @tparam InputValue  Type of the input values
 * @tparam StoreKey    Type of the key being stored in the state store (needed to support onEventTimer cursoring)
 * @tparam TimerKey    Type of the timer key
 * @tparam OutputKey   Type of the output keys
 * @tparam OutputValue Type of the output values
 * }}}
 */
//TODO: Create variant for when there are no timers (e.g. avoid the extra time params and need to specify a timer store
@Beta
abstract class FinatraTransformer[InputKey, InputValue, StoreKey, TimerKey, OutputKey, OutputValue](
  commitInterval: Duration = null, //TODO: This field is currently only used by one external customer (but unable to @deprecate a constructor param). Will remove from caller and here in followup Phab.
  cacheTimers: Boolean = true,
  throttlingResetDuration: Duration = 3.seconds,
  disableTimers: Boolean = false,
  timerStoreName: String,
  statsReceiver: StatsReceiver = LoadedStatsReceiver) //TODO
    extends Transformer[InputKey, InputValue, (OutputKey, OutputValue)]
    with OnInit
    with OnClose
    with StateStoreImplicits
    with IteratorImplicits
    with ProcessorContextLogging {

  /* Private Mutable */

  @volatile private var _context: ProcessorContext = _
  @volatile private var cancellableThrottlingResetTimer: Cancellable = _
  @volatile private var processingTimerCancellable: Cancellable = _
  @volatile private var nextTimer: Long = Long.MaxValue //Maintain to avoid iterating timerStore every time fireTimers is called

  //TODO: Persist cursor in stateStore to avoid duplicate cursored work after a restart
  @volatile private var throttled: Boolean = false
  @volatile private var lastThrottledCursor: Option[StoreKey] = None

  /* Private */

  private val watermarkTracker = new WatermarkTracker
  private val cachedTimers = new ObjectHashSet[Timer[TimerKey]](16)
  private val finatraKeyValueStores =
    scala.collection.mutable.Map[String, FinatraKeyValueStore[_, _]]()

  protected[finatra] final val timersStore = if (disableTimers) {
    null
  } else {
    getKeyValueStore[Timer[TimerKey], Array[Byte]](timerStoreName)
  }

  /* Abstract */

  protected[finatra] def onMessage(messageTime: Time, key: InputKey, value: InputValue): Unit

  protected def onProcessingTimer(time: TimerTime): Unit = {}

  /**
   * Callback for when an Event timer is ready for processing
   *
   * @return TimerResult indicating if this timer should be retained or deleted
   */
  protected def onEventTimer(
    time: Time,
    metadata: TimerMetadata,
    key: TimerKey,
    cursor: Option[StoreKey]
  ): TimerResult[StoreKey] = {
    warn(s"Unhandled timer $time $metadata $key")
    DeleteTimer()
  }

  /* Protected */

  final override def init(processorContext: ProcessorContext): Unit = {
    _context = processorContext

    for ((name, store) <- finatraKeyValueStores) {
      store.init(processorContext, null)
    }

    if (!disableTimers) {
      cancellableThrottlingResetTimer = _context
        .schedule(
          throttlingResetDuration.inMillis,
          PunctuationType.WALL_CLOCK_TIME,
          new Punctuator {
            override def punctuate(timestamp: TimerTime): Unit = {
              resetThrottled()
              fireEventTimeTimers()
            }
          }
        )

      findAndSetNextTimer()
      cacheTimersIfEnabled()
    }

    onInit()
  }

  override protected def processorContext: ProcessorContext = _context

  final override def transform(k: InputKey, v: InputValue): (OutputKey, OutputValue) = {
    if (watermarkTracker.track(_context.topic(), _context.timestamp)) {
      fireEventTimeTimers()
    }

    debug(s"onMessage ${_context.timestamp.iso8601Millis} $k $v")
    onMessage(Time(_context.timestamp()), k, v)

    null
  }

  final override def close(): Unit = {
    setNextTimerTime(0)
    cachedTimers.clear()
    watermarkTracker.reset()

    if (cancellableThrottlingResetTimer != null) {
      cancellableThrottlingResetTimer.cancel()
      cancellableThrottlingResetTimer = null
    }

    if (processingTimerCancellable != null) {
      processingTimerCancellable.cancel()
      processingTimerCancellable = null
    }

    for ((name, store) <- finatraKeyValueStores) {
      store.close()
      FinatraStoresGlobalManager.removeStore(store)
    }

    onClose()
  }

  final protected def getKeyValueStore[KK: ClassTag, VV](
    name: String
  ): FinatraKeyValueStore[KK, VV] = {
    val store = new FinatraKeyValueStore[KK, VV](name, statsReceiver)
    val previousStore = finatraKeyValueStores.put(name, store)
    FinatraStoresGlobalManager.addStore(store)
    assert(previousStore.isEmpty, s"getKeyValueStore was called for store $name more than once")

    // Initialize stores that are still using the "lazy val store" pattern
    if (processorContext != null) {
      store.init(processorContext, null)
    }

    store
  }

  //TODO: Add a forwardOnCommit which just takes a key
  final protected def forward(key: OutputKey, value: OutputValue): Unit = {
    trace(f"${"Forward:"}%-20s $key $value")
    _context.forward(key, value)
  }

  final protected def forward(key: OutputKey, value: OutputValue, timestamp: Long): Unit = {
    trace(f"${"Forward:"}%-20s $key $value @${new DateTime(timestamp)}")
    ProcessorContextUtils.setTimestamp(_context, timestamp)
    _context.forward(key, value)
  }

  final protected def watermark: Long = {
    watermarkTracker.watermark
  }

  final protected def addEventTimeTimer(
    time: Time,
    metadata: TimerMetadata,
    key: TimerKey
  ): Unit = {
    trace(
      f"${"AddEventTimer:"}%-20s ${metadata.getClass.getSimpleName}%-12s Key $key Timer ${time.millis.iso8601Millis}"
    )
    val timer = Timer(time = time.millis, metadata = metadata, key = key)
    if (cacheTimers && cachedTimers.contains(timer)) {
      trace(s"Deduped unkeyed timer: $timer")
    } else {
      timersStore.put(timer, Array.emptyByteArray)
      if (time.millis < nextTimer) {
        setNextTimerTime(time.millis)
      }
      if (cacheTimers) {
        cachedTimers.add(timer)
      }
    }
  }

  final protected def addProcessingTimeTimer(duration: Duration): Unit = {
    assert(
      processingTimerCancellable == null,
      "NonPersistentProcessingTimer already set. We currently only support a single processing timer being set through addProcessingTimeTimer."
    )
    processingTimerCancellable =
      processorContext.schedule(duration.inMillis, PunctuationType.WALL_CLOCK_TIME, new Punctuator {
        override def punctuate(time: Long): Unit = {
          onProcessingTimer(time)
        }
      })
  }

  final protected def deleteOrRetainTimer(
    iterator: KeyValueIterator[StoreKey, _],
    onDeleteTimer: => Unit = () => ()
  ): TimerResult[StoreKey] = {
    if (iterator.hasNext) {
      RetainTimer(stateStoreCursor = iterator.peekNextKeyOpt, throttled = true)
    } else {
      onDeleteTimer
      DeleteTimer()
    }
  }

  /* Private */

  private def fireEventTimeTimers(): Unit = {
    trace(
      s"FireTimers watermark ${watermark.iso8601Millis} nextTimer ${nextTimer.iso8601Millis} throttled $throttled"
    )
    if (!disableTimers && !isThrottled && watermark >= nextTimer) {
      val timerIterator = timersStore.all()
      try {
        timerIterator.asScala
          .takeWhile { timerAndEmptyValue =>
            !isThrottled && watermark >= timerAndEmptyValue.key.time
          }
          .foreach { timerAndEmptyValue =>
            fireEventTimeTimer(timerAndEmptyValue.key)
          }
      } finally {
        timerIterator.close()
        findAndSetNextTimer() //TODO: Optimize by avoiding the need to re-read from the timersStore iterator
      }
    }
  }

  //Note: LastThrottledCursor is shared per Task. However, since the timers are sorted, we should only be cursoring the head timer at a time.
  private def fireEventTimeTimer(timer: Timer[TimerKey]): Unit = {
    trace(
      s"fireEventTimeTimer ${timer.metadata.getClass.getName} key: ${timer.key} timerTime: ${timer.time.iso8601Millis}"
    )

    onEventTimer(
      time = Time(timer.time),
      metadata = timer.metadata,
      key = timer.key,
      lastThrottledCursor
    ) match {
      case DeleteTimer(throttledResult) =>
        lastThrottledCursor = None
        throttled = throttledResult

        timersStore.deleteWithoutGettingPriorValue(timer)
        if (cacheTimers) {
          cachedTimers.remove(timer)
        }
      case RetainTimer(stateStoreCursor, throttledResult) =>
        lastThrottledCursor = stateStoreCursor
        throttled = throttledResult
    }
  }

  private def findAndSetNextTimer(): Unit = {
    val iterator = timersStore.all()
    try {
      if (iterator.hasNext) {
        setNextTimerTime(iterator.peekNextKey.time)
      } else {
        setNextTimerTime(Long.MaxValue)
      }
    } finally {
      iterator.close()
    }
  }

  private def setNextTimerTime(time: TimerTime): Unit = {
    nextTimer = time
    if (time != Long.MaxValue) {
      trace(s"NextTimer: ${nextTimer.iso8601Millis}")
    }
  }

  private def cacheTimersIfEnabled(): Unit = {
    if (cacheTimers) {
      val iterator = timersStore.all()
      try {
        for (timerKeyValue <- iterator.asScala) {
          val timer = timerKeyValue.key
          cachedTimers.add(timer)
        }
      } finally {
        iterator.close()
      }
    }
  }

  private def resetThrottled(): Unit = {
    throttled = false
  }

  private def isThrottled: Boolean = {
    throttled
  }
}
