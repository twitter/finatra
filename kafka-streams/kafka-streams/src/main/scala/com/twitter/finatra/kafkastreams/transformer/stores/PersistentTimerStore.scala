package com.twitter.finatra.kafkastreams.transformer.stores

import com.google.common.annotations.Beta
import com.twitter.finatra.kafkastreams.transformer.domain.{Time, TimerMetadata}
import com.twitter.finatra.kafkastreams.transformer.stores.internal.Timer

@Beta
class PersistentTimerStore[TimerKey](
  timersStore: FinatraKeyValueStore[Timer[TimerKey], Array[Byte]],
  onTimer: (Time, TimerMetadata, TimerKey) => Unit,
  maxTimerFiresPerWatermark: Int)
    extends PersistentTimerValueStore[TimerKey, Array[Byte]](
      timersStore,
      {
        case (time, timerMetadata, timerKey, _) =>
          onTimer(time, timerMetadata, timerKey)
      },
      maxTimerFiresPerWatermark
    ) {

  /* Public */
  def addTimer(time: Time, metadata: TimerMetadata, key: TimerKey): Unit = {
    addTimer(time, metadata, key, Array.emptyByteArray)
  }
}
