package com.twitter.finatra.streams.transformer

import com.google.common.annotations.Beta
import com.twitter.finagle.stats.StatsReceiver
import com.twitter.finatra.kafka.utils.ConfigUtils
import com.twitter.finatra.kafkastreams.internal.utils.ProcessorContextLogging
import com.twitter.finatra.streams.flags.FinatraTransformerFlags
import com.twitter.finatra.streams.stores.FinatraKeyValueStore
import com.twitter.finatra.streams.transformer.FinatraTransformer.TimerTime
import com.twitter.finatra.streams.transformer.domain.{Time, Watermark}
import com.twitter.finatra.streams.transformer.internal.{OnClose, OnInit}
import com.twitter.finatra.streams.transformer.watermarks.internal.WatermarkManager
import com.twitter.finatra.streams.transformer.watermarks.{
  DefaultWatermarkAssignor,
  WatermarkAssignor
}
import com.twitter.util.Duration
import org.apache.kafka.streams.kstream.Transformer
import org.apache.kafka.streams.processor.{
  Cancellable,
  ProcessorContext,
  PunctuationType,
  Punctuator,
  To
}
import org.joda.time.DateTime
import scala.reflect.ClassTag

/**
 * A KafkaStreams Transformer offering an upgraded API over the built in Transformer interface
 *
 * Note: In time, this class will be merged with FinatraTransformer
 *
 * @tparam InputKey    Type of the input keys
 * @tparam InputValue  Type of the input values
 * @tparam OutputKey   Type of the output keys
 * @tparam OutputValue Type of the output values
 */
@Beta
abstract class FinatraTransformerV2[InputKey, InputValue, OutputKey, OutputValue](
  statsReceiver: StatsReceiver,
  watermarkAssignor: WatermarkAssignor[InputKey, InputValue] =
    new DefaultWatermarkAssignor[InputKey, InputValue])
    extends Transformer[InputKey, InputValue, (OutputKey, OutputValue)]
    with OnInit
    with OnWatermark
    with OnClose
    with ProcessorContextLogging {

  private val finatraKeyValueStores =
    scala.collection.mutable.Map[String, FinatraKeyValueStore[_, _]]()

  private var watermarkManager: WatermarkManager[InputKey, InputValue] = _

  /* Private Mutable */

  @volatile private var _context: ProcessorContext = _
  @volatile private var watermarkTimerCancellable: Cancellable = _

  /* Abstract */

  protected[finatra] def onMessage(messageTime: Time, key: InputKey, value: InputValue): Unit

  /* Protected */

  override protected def processorContext: ProcessorContext = _context

  final override def init(processorContext: ProcessorContext): Unit = {
    _context = processorContext

    watermarkManager = new WatermarkManager[InputKey, InputValue](
      onWatermark = this,
      watermarkAssignor = watermarkAssignor,
      emitWatermarkPerMessage = emitWatermarkPerMessage(_context)
    )

    for ((name, store) <- finatraKeyValueStores) {
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

  override def onWatermark(watermark: Watermark): Unit = {
    trace(s"onWatermark $watermark")
  }

  final override def transform(k: InputKey, v: InputValue): (OutputKey, OutputValue) = {
    debug(s"onMessage ${_context.timestamp.iso8601Millis} $k -> $v")
    val time = Time(_context.timestamp())
    watermarkManager.onMessage(time, _context.topic(), k, v)
    onMessage(time, k, v)

    null
  }

  final override def close(): Unit = {
    if (watermarkTimerCancellable != null) {
      watermarkTimerCancellable.cancel()
      watermarkTimerCancellable = null
    }
    watermarkManager.close()

    for ((name, store) <- finatraKeyValueStores) {
      store.close()
    }

    onClose()
  }

  final protected def getKeyValueStore[KK: ClassTag, VV](
    name: String
  ): FinatraKeyValueStore[KK, VV] = {
    val store = new FinatraKeyValueStore[KK, VV](name, statsReceiver)
    val previousStore = finatraKeyValueStores.put(name, store)
    assert(previousStore.isEmpty, s"getKeyValueStore was called for store $name more than once")

    // Initialize stores that are still using the "lazy val store" pattern
    if (processorContext != null) {
      store.init(processorContext, null)
    }

    store
  }

  final protected def forward(key: OutputKey, value: OutputValue): Unit = {
    trace(f"${"Forward:"}%-20s $key $value")
    _context.forward(key, value)
  }

  final protected def forward(key: OutputKey, value: OutputValue, timestamp: Long): Unit = {
    trace(f"${"Forward:"}%-20s $key $value @${new DateTime(timestamp)}")
    _context.forward(key, value, To.all().withTimestamp(timestamp))
  }

  final protected def watermark: Watermark = {
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

  private def emitWatermarkPerMessage(processorContext: ProcessorContext): Boolean = {
    ConfigUtils
      .getConfigOrElse(
        processorContext.appConfigs,
        FinatraTransformerFlags.EmitWatermarkPerMessage,
        "false"
      ).toBoolean
  }
}
