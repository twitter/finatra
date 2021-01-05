package com.twitter.finatra.kafkastreams.dsl

import com.twitter.finatra.kafkastreams.KafkaStreamsTwitterServer
import com.twitter.finatra.kafkastreams.config.FinatraTransformerFlags
import com.twitter.finatra.kafkastreams.transformer.FinatraTransformer
import com.twitter.util.Duration
import org.apache.kafka.streams.kstream.Transformer
import org.apache.kafka.streams.kstream.internals.ProducedInternal
import org.apache.kafka.streams.scala.kstream.{KStream, Produced}
import scala.reflect.ClassTag

trait FinatraDslDelay extends KafkaStreamsTwitterServer with FinatraTransformerFlags {
  implicit class DelayKeyValueStream[K: ClassTag, V](kstream: KStream[K, V]) {

    /**
     * Inserts a transformer into the stream which delays events.
     *
     * If the delay is long, use this -- it uses a timer store -- Events come in and are placed
     * into a message store. A timer is placed into a timer store, which when triggered will forward the event that
     * was stored in the message store downstream.
     *
     * "long" is somewhat subjective. If your delay is on the order of seconds or lower, then prefer to use the
     * sleep based timer below to avoid using local disk resources and creating a changelog topic.
     * If the delay is on the order of hours, then prefer to use the store based timer so that
     * events persist in the store across service restarts.
     *
     * @param delay How long to delay events
     * @param storeKey A unique key to differentiate multiple delay stores in a single topology
     * @param produced key/value Serdes for the key and value.
     */
    def delayWithStore(
      delay: Duration,
      storeKey: String
    )(
      implicit produced: Produced[K, V]
    ): KStream[K, V] = {
      val producedInternal = new ProducedInternal(produced)

      val timerStore =
        FinatraTransformer.timerValueStore(
          s"delay_timerstore_$storeKey",
          producedInternal.keySerde,
          producedInternal.valueSerde,
          streamsStatsReceiver)

      kafkaStreamsBuilder.addStateStore(timerStore)

      val supplier: () => Transformer[K, V, (K, V)] = () =>
        new TimerStoreDelayTransformer[K, V](delay, timerStore.name)

      kstream.transform(supplier, timerStore.name)

    }

    /**
     * Inserts a transformer into the stream which delays events.
     *
     * If the delay is short, use this-- just compare the wall clock time to the message time and
     * sleep for long enough so that the wall clock time will message time + delay when the sleep
     * is finished. If using this form of delay, the # of stream threads across all instances
     * must be greater than or equal to the # of partitions so that the sleep for one partition
     * doesn't delay reading other partitions.
     *
     * "short" is somewhat subjective. If your delay is on the order of seconds or lower, then prefer to use the
     * sleep based timer below to avoid using local disk resources and creating a changelog topic.
     * If the delay is on the order of hours, then prefer to use the store based timer so that
     * events persist in the store across service restarts.
     *
     * @param delay How long to delay events
     *
     */
    def delayBySleep(delay: Duration): KStream[K, V] = {
      val supplier: () => Transformer[K, V, (K, V)] = () => new SleepDelayTransformer(delay)

      kstream.transform(supplier)
    }
  }

}
