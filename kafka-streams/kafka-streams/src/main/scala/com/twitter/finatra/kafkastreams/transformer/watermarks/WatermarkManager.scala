package com.twitter.finatra.kafkastreams.transformer.watermarks

import com.twitter.finatra.kafkastreams.transformer.domain.Time
import com.twitter.finatra.kafkastreams.transformer.lifecycle.OnWatermark
import com.twitter.util.logging.Logging
import org.apache.kafka.streams.processor.TaskId

/**
 * WatermarkManager coordinates with a Transformers WatermarkAssignor to keep track of the latest assigned
 * watermark and the last emitted watermark.
 *
 * @param taskId TaskId of the FinatraTransformer being managed used for internal logging
 * @param transformerName Transformer name of the FinatraTransformer being managed used for internal logging
 * @param onWatermark OnWatermark callback which is called when a new watermark is emitted
 * @param watermarkAssignor The WatermarkAssignor used in the FinatraTransformer being managed.
 * @param emitWatermarkPerMessage Whether to check if a new watermark needs to be emitted after each
 *                                message is read in onMessage. If false, callOnWatermarkIfChanged must
 *                                be called to check if a new watermark is to be emitted.
 * @tparam K Message key for the FinatraTransformer being managed
 * @tparam V Message value for the FinatraTransformer being managed
 */
class WatermarkManager[K, V](
  taskId: TaskId,
  transformerName: String,
  onWatermark: OnWatermark,
  watermarkAssignor: WatermarkAssignor[K, V],
  emitWatermarkPerMessage: Boolean)
    extends Logging {

  @volatile private var lastEmittedWatermark = Watermark.unknown

  /* Public */

  def close(): Unit = {
    setLastEmittedWatermark(Watermark(0L))
  }

  def watermark: Watermark = {
    lastEmittedWatermark
  }

  def onMessage(messageTime: Time, topic: String, key: K, value: V): Unit = {
    watermarkAssignor.onMessage(topic = topic, timestamp = messageTime, key = key, value = value)

    if (lastEmittedWatermark == Watermark.unknown || emitWatermarkPerMessage) {
      callOnWatermarkIfChanged()
    }
  }

  def callOnWatermarkIfChanged(): Unit = {
    val latestAssignedWatermark = watermarkAssignor.getWatermark
    trace(s"callOnWatermarkIfChanged $transformerName $taskId $latestAssignedWatermark")
    if (latestAssignedWatermark.timeMillis > lastEmittedWatermark.timeMillis) {
      // It's important to call setLastEmittedWatermark before calling onWatermark since onWatermark may in turn call a callback that wants to read the current watermark...
      setLastEmittedWatermark(latestAssignedWatermark)
      onWatermark.onWatermark(latestAssignedWatermark)
    }
  }

  protected[kafkastreams] def setLastEmittedWatermark(newWatermark: Watermark): Unit = {
    lastEmittedWatermark = newWatermark
  }
}
