package com.twitter.finatra.streams.transformer

import com.google.common.annotations.Beta
import com.twitter.finagle.stats.StatsReceiver
import com.twitter.finatra.kafka.utils.ConfigUtils
import com.twitter.finatra.kafkastreams.internal.utils.ProcessorContextLogging
import com.twitter.finatra.streams.config.DefaultTopicConfig
import com.twitter.finatra.streams.flags.FinatraTransformerFlags
import com.twitter.finatra.streams.stores.FinatraKeyValueStore
import com.twitter.finatra.streams.stores.internal.{FinatraKeyValueStoreImpl, FinatraStoresGlobalManager}
import com.twitter.finatra.streams.transformer.FinatraTransformer.TimerTime
import com.twitter.finatra.streams.transformer.domain.{Time, Watermark}
import com.twitter.finatra.streams.transformer.internal.domain.{Timer, TimerSerde}
import com.twitter.finatra.streams.transformer.internal.{OnClose, OnFlush, OnInit}
import com.twitter.finatra.streams.transformer.watermarks.internal.WatermarkManager
import com.twitter.finatra.streams.transformer.watermarks.{DefaultWatermarkAssignor, WatermarkAssignor}
import com.twitter.util.Duration
import org.apache.kafka.common.serialization.{Serde, Serdes}
import org.apache.kafka.streams.kstream.Transformer
import org.apache.kafka.streams.processor.{Cancellable, ProcessorContext, PunctuationType, Punctuator, To}
import org.apache.kafka.streams.state.{KeyValueStore, StoreBuilder, Stores}
import scala.collection.mutable
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
 * A KafkaStreams Transformer offering an upgraded API over the built in Transformer interface.
 *
 * This Transformer differs from the built in Transformer interface by exposing an [onMessage]
 * interface that is used to process incoming messages.  Within [onMessage] you may use the
 * [forward] method to emit 0 or more records.
 *
 * This transformer also manages watermarks(see [WatermarkManager]), and extends [OnWatermark] which
 * allows you to track the passage of event time.
 *
 * @tparam InputKey    Type of the input keys
 * @tparam InputValue  Type of the input values
 * @tparam OutputKey   Type of the output keys
 * @tparam OutputValue Type of the output values
 */
@Beta
abstract class FinatraTransformer[InputKey, InputValue, OutputKey, OutputValue](
  statsReceiver: StatsReceiver,
  watermarkAssignor: WatermarkAssignor[InputKey, InputValue] =
  new DefaultWatermarkAssignor[InputKey, InputValue])
  extends Transformer[InputKey, InputValue, (OutputKey, OutputValue)]
    with OnInit
    with OnWatermark
    with OnClose
    with OnFlush
    with ProcessorContextLogging {

  protected[streams] val finatraKeyValueStoresMap: mutable.Map[String, FinatraKeyValueStore[_, _]] =
    scala.collection.mutable.Map[String, FinatraKeyValueStore[_, _]]()

  /* Private Mutable */

  @volatile private var _context: ProcessorContext = _
  @volatile private var watermarkTimerCancellable: Cancellable = _
  @volatile private var watermarkManager: WatermarkManager[InputKey, InputValue] = _

  /* Abstract */

  /**
   * Callback method which is called for every message in the stream this Transformer is attached to.
   * Implementers of this method may emit 0 or more records by using the processorContext.
   *
   * @param messageTime the time of the message
   * @param key the key of the message
   * @param value the value of the message
   */
  protected[finatra] def onMessage(messageTime: Time, key: InputKey, value: InputValue): Unit

  /* Protected */

  override protected def processorContext: ProcessorContext = _context

  final override def init(processorContext: ProcessorContext): Unit = {
    _context = processorContext

    watermarkManager = new WatermarkManager[InputKey, InputValue](
      taskId = processorContext.taskId(),
      transformerName = this.getClass.getSimpleName,
      onWatermark = this,
      watermarkAssignor = watermarkAssignor,
      emitWatermarkPerMessage = shouldEmitWatermarkPerMessage(_context))

    for ((name, store) <- finatraKeyValueStoresMap) {
      store.init(processorContext, null)
    }

    val autoWatermarkInterval = parseAutoWatermarkInterval(_context).inMillis
    if (autoWatermarkInterval > 0) {
      watermarkTimerCancellable = _context.schedule(
        autoWatermarkInterval,
        PunctuationType.WALL_CLOCK_TIME,
        new Punctuator {
          override def punctuate(timestamp: TimerTime): Unit = {
            watermarkManager.callOnWatermarkIfChanged()
          }
        }
      )
    }

    onInit()
  }

  override def onFlush(): Unit = {
    super.onFlush()
    watermarkManager.callOnWatermarkIfChanged()
  }

  override def onWatermark(watermark: Watermark): Unit = {
    trace(s"onWatermark $watermark")
  }

  final override def transform(k: InputKey, v: InputValue): (OutputKey, OutputValue) = {
    /* Note: It's important to save off the message time before watermarkManager.onMessage is called
       which can trigger persistent timers to fire, which can cause messages to be forwarded, which
       can cause context.timestamp to be mutated to the forwarded message timestamp :-( */
    val messageTime = Time(_context.timestamp())

    watermarkManager.onMessage(messageTime, _context.topic(), k, v)
    debug(s"onMessage LastEmitted $watermark MessageTime $messageTime $k -> $v")
    onMessage(messageTime, k, v)
    null
  }

  final override def close(): Unit = {
    if (watermarkTimerCancellable != null) {
      watermarkTimerCancellable.cancel()
      watermarkTimerCancellable = null
    }
    watermarkManager.close()

    for ((name, store) <- finatraKeyValueStoresMap) {
      store.close()
      FinatraStoresGlobalManager.removeStore(store)
    }

    onClose()
  }

  final protected def getKeyValueStore[KK: ClassTag, VV](
    name: String
  ): FinatraKeyValueStore[KK, VV] = {
    val store = new FinatraKeyValueStoreImpl[KK, VV](name, statsReceiver)

    val previousStore = finatraKeyValueStoresMap.put(name, store)
    assert(previousStore.isEmpty, s"getKeyValueStore was called for store $name more than once")
    FinatraStoresGlobalManager.addStore(store)

    // Initialize stores that are still using the "lazy val store" pattern
    if (processorContext != null) {
      store.init(processorContext, null)
    }

    store
  }

  final protected def forward(key: OutputKey, value: OutputValue): Unit = {
    debug(s"Forward ${_context.timestamp().iso8601Millis} $key $value")
    _context.forward(key, value)
  }

  final protected def forward(key: OutputKey, value: OutputValue, timestamp: Long): Unit = {
    if (timestamp <= 10000) {
      warn(s"Forward SMALL TIMESTAMP: $timestamp $key $value")
    } else {
      debug(s"Forward ${timestamp.iso8601Millis} $key $value")
    }

    _context.forward(key, value, To.all().withTimestamp(timestamp))
  }

  final protected[finatra] def watermark: Watermark = {
    watermarkManager.watermark
  }

  private def parseAutoWatermarkInterval(processorContext: ProcessorContext): Duration = {
    Duration.parse(
      ConfigUtils.getConfigOrElse(
        processorContext.appConfigs,
        FinatraTransformerFlags.AutoWatermarkInterval,
        "100.milliseconds"
      )
    )
  }

  private def shouldEmitWatermarkPerMessage(processorContext: ProcessorContext): Boolean = {
    ConfigUtils
      .getConfigOrElse(
        configs = processorContext.appConfigs,
        key = FinatraTransformerFlags.EmitWatermarkPerMessage,
        default = "false").toBoolean
  }
}
