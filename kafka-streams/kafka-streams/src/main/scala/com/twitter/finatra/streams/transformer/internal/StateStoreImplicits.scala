package com.twitter.finatra.streams.transformer.internal

import com.twitter.finagle.stats.Stat
import com.twitter.finatra.kafkastreams.internal.utils.ProcessorContextLogging
import com.twitter.util.Stopwatch
import org.apache.kafka.streams.state.KeyValueStore

trait StateStoreImplicits extends ProcessorContextLogging {

  /* ------------------------------------------ */
  implicit class RichKeyIntValueStore[SK](keyValueStore: KeyValueStore[SK, Int]) {

    /**
     * @return the new value associated with the specified key
     */
    final def increment(key: SK, amount: Int): Int = {
      val existingCount = keyValueStore.get(key)
      val newCount = existingCount + amount
      trace(s"keyValueStore.put($key, $newCount)")
      keyValueStore.put(key, newCount)
      newCount
    }

    /**
     * @return the new value associated with the specified key
     */
    final def increment(key: SK, amount: Int, getStat: Stat, putStat: Stat): Int = {
      val getElapsed = Stopwatch.start()
      val existingCount = keyValueStore.get(key)
      val getElapsedMillis = getElapsed.apply().inMillis
      getStat.add(getElapsedMillis)
      if (getElapsedMillis > 10) {
        warn(s"SlowGet $getElapsedMillis ms for key $key")
      }

      val newCount = existingCount + amount

      val putElapsed = Stopwatch.start()
      keyValueStore.put(key, newCount)
      putStat.add(putElapsed.apply().inMillis)

      newCount
    }
  }
}
