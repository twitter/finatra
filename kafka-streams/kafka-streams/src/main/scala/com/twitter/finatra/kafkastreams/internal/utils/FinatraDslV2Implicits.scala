package com.twitter.finatra.kafkastreams.internal.utils

import com.twitter.app.Flag
import com.twitter.finagle.stats.StatsReceiver
import com.twitter.finatra.kafka.serde.ScalaSerdes
import com.twitter.finatra.kafkastreams.internal.ScalaStreamsImplicits
import com.twitter.finatra.streams.config.DefaultTopicConfig
import com.twitter.finatra.streams.transformer.domain.{
  CompositeKey,
  FixedTimeWindowedSerde,
  TimeWindowed,
  WindowedValue
}
import com.twitter.finatra.streams.transformer.{
  CompositeSumAggregator,
  FinatraTransformer,
  SumAggregator
}
import com.twitter.inject.Logging
import com.twitter.util.Duration
import org.apache.kafka.common.serialization.Serde
import org.apache.kafka.streams.kstream.Transformer
import org.apache.kafka.streams.scala.kstream.{KStream => KStreamS}
import org.apache.kafka.streams.state.Stores
import org.apache.kafka.streams.{KafkaStreams, StreamsBuilder}
import scala.reflect.ClassTag

@deprecated("Use FinatraDslWindowedAggregations", "1/7/2019")
trait FinatraDslV2Implicits extends ScalaStreamsImplicits {

  protected def kafkaStreams: KafkaStreams

  protected def streamsStatsReceiver: StatsReceiver

  protected def kafkaStreamsBuilder: StreamsBuilder

  protected def commitInterval: Flag[Duration]

  /* ---------------------------------------- */
  implicit class FinatraKStream[K: ClassTag](inner: KStreamS[K, Int]) extends Logging {

    /**
     * For each unique key, sum the values in the stream that occurred within a given time window
     * and store those values in a StateStore named stateStore.
     *
     * A TimeWindow is a tumbling window of fixed length defined by the windowSize parameter.
     *
     * A Window is closed after event time passes the end of a TimeWindow + allowedLateness.
     *
     * After a window is closed it is forwarded out of this transformer with a
     * [[WindowedValue.resultState]] of [[com.twitter.finatra.streams.transformer.domain.WindowClosed]]
     *
     * If a record arrives after a window is closed it is immediately forwarded out of this
     * transformer with a [[WindowedValue.resultState]] of [[com.twitter.finatra.streams.transformer.domain.Restatement]]
     *
     * @param stateStore the name of the StateStore used to maintain the counts.
     * @param windowSize splits the stream of data into buckets of data of windowSize,
     *                   based on the timestamp of each message.
     * @param allowedLateness allow messages that are upto this amount late to be added to the
     *                        store, otherwise they are emitted as restatements.
     * @param queryableAfterClose allow state to be queried upto this amount after the window is
     *                            closed.
     * @param keyRangeStart The minimum value that will be stored in the key based on binary sort order.
     * @param keySerde Serde for the keys in the StateStore.
     * @return a stream of Keys for a particular timewindow, and the sum of the values for that key
     *         within a particular timewindow.
     */
    def sum(
      stateStore: String,
      windowSize: Duration,
      allowedLateness: Duration,
      queryableAfterClose: Duration,
      keyRangeStart: K,
      keySerde: Serde[K]
    ): KStreamS[TimeWindowed[K], WindowedValue[Int]] = {

      kafkaStreamsBuilder.addStateStore(
        Stores
          .keyValueStoreBuilder(
            Stores.persistentKeyValueStore(stateStore),
            FixedTimeWindowedSerde(keySerde, windowSize),
            ScalaSerdes.Int
          )
          .withLoggingEnabled(DefaultTopicConfig.FinatraChangelogConfig)
      )

      //Note: The TimerKey is a WindowStartMs value used by MultiAttributeCountAggregator
      val timerStore = FinatraTransformer.timerStore(s"$stateStore-TimerStore", ScalaSerdes.Long)
      kafkaStreamsBuilder.addStateStore(timerStore)

      val transformerSupplier = () =>
        new SumAggregator[K, Int](
          commitInterval = commitInterval(),
          keyRangeStart = keyRangeStart,
          statsReceiver = streamsStatsReceiver,
          stateStoreName = stateStore,
          timerStoreName = timerStore.name(),
          windowSize = windowSize,
          allowedLateness = allowedLateness,
          queryableAfterClose = queryableAfterClose,
          countToAggregate = (key, count) => count,
          windowStart = (messageTime, key, value) =>
            TimeWindowed.windowStart(messageTime, windowSize)
        )

      inner.transform(transformerSupplier, stateStore, timerStore.name)
    }
  }

  /* ---------------------------------------- */
  implicit class FinatraKeyToWindowedValueStream[K, TimeWindowedType <: TimeWindowed[Int]](
    inner: KStreamS[K, TimeWindowedType])
      extends Logging {

    def sum(
      stateStore: String,
      allowedLateness: Duration,
      queryableAfterClose: Duration,
      emitOnClose: Boolean,
      windowSize: Duration,
      keyRangeStart: K,
      keySerde: Serde[K]
    ): KStreamS[TimeWindowed[K], WindowedValue[Int]] = {
      kafkaStreamsBuilder.addStateStore(
        Stores
          .keyValueStoreBuilder(
            Stores.persistentKeyValueStore(stateStore),
            FixedTimeWindowedSerde(keySerde, windowSize),
            ScalaSerdes.Int
          )
          .withLoggingEnabled(DefaultTopicConfig.FinatraChangelogConfig)
      )

      //Note: The TimerKey is a WindowStartMs value used by MultiAttributeCountAggregator
      val timerStore = FinatraTransformer.timerStore(s"$stateStore-TimerStore", ScalaSerdes.Long)
      kafkaStreamsBuilder.addStateStore(timerStore)

      val transformerSupplier = (
        () =>
          new SumAggregator[K, TimeWindowed[Int]](
            commitInterval = commitInterval(),
            keyRangeStart = keyRangeStart,
            statsReceiver = streamsStatsReceiver,
            stateStoreName = stateStore,
            timerStoreName = timerStore.name(),
            windowSize = windowSize,
            allowedLateness = allowedLateness,
            emitOnClose = emitOnClose,
            queryableAfterClose = queryableAfterClose,
            countToAggregate = (key, windowedValue) => windowedValue.value,
            windowStart = (messageTime, key, windowedValue) => windowedValue.start
          )
        ).asInstanceOf[() => Transformer[
        K,
        TimeWindowedType,
        (TimeWindowed[K], WindowedValue[Int])]] //Coerce TimeWindowed[Int] into TimeWindowedType :-/

      inner
        .transform(transformerSupplier, stateStore, timerStore.name)
    }
  }

  /* ---------------------------------------- */
  implicit class FinatraCompositeKeyKStream[CompositeKeyType <: CompositeKey[_, _]: ClassTag](
    inner: KStreamS[CompositeKeyType, Int])
      extends Logging {

    /**
     * For each unique composite key, sum the values in the stream that occurred within a given time window.
     *
     * A composite key is a multi part key that can be efficiently range scanned using the
     * primary key, or the primary key and the secondary key.
     *
     * A TimeWindow is a tumbling window of fixed length defined by the windowSize parameter.
     *
     * A Window is closed after event time passes the end of a TimeWindow + allowedLateness.
     *
     * After a window is closed it is forwarded out of this transformer with a
     * [[WindowedValue.resultState]] of [[com.twitter.finatra.streams.transformer.domain.WindowClosed]]
     *
     * If a record arrives after a window is closed it is immediately forwarded out of this
     * transformer with a [[WindowedValue.resultState]] of [[com.twitter.finatra.streams.transformer.domain.Restatement]]
     *
     * @param stateStore          the name of the StateStore used to maintain the counts.
     * @param windowSize          splits the stream of data into buckets of data of windowSize,
     *                            based on the timestamp of each message.
     * @param allowedLateness     allow messages that are upto this amount late to be added to the
     *                            store, otherwise they are emitted as restatements.
     * @param queryableAfterClose allow state to be queried upto this amount after the window is
     *                            closed.
     * @param emitOnClose         whether or not to emit a record when the window is closed.
     * @param compositeKeyRangeStart The minimum value that will be stored in the key based on binary sort order.
     * @param compositeKeySerde serde for the composite key in the StateStore.
     * @tparam PrimaryKey the type for the primary key
     * @tparam SecondaryKey the type for the secondary key
     *
     * @return
     */
    def compositeSum[PrimaryKey, SecondaryKey](
      stateStore: String,
      windowSize: Duration,
      allowedLateness: Duration,
      queryableAfterClose: Duration,
      emitOnClose: Boolean,
      compositeKeyRangeStart: CompositeKey[PrimaryKey, SecondaryKey],
      compositeKeySerde: Serde[CompositeKeyType]
    ): KStreamS[TimeWindowed[PrimaryKey], WindowedValue[scala.collection.Map[SecondaryKey, Int]]] = {

      kafkaStreamsBuilder.addStateStore(
        Stores
          .keyValueStoreBuilder(
            Stores.persistentKeyValueStore(stateStore),
            FixedTimeWindowedSerde(compositeKeySerde, windowSize),
            ScalaSerdes.Int
          )
          .withLoggingEnabled(DefaultTopicConfig.FinatraChangelogConfig)
      )

      //Note: The TimerKey is a WindowStartMs value used by MultiAttributeCountAggregator
      val timerStore = FinatraTransformer.timerStore(s"$stateStore-TimerStore", ScalaSerdes.Long)
      kafkaStreamsBuilder.addStateStore(timerStore)

      val transformerSupplier = (
        () =>
          new CompositeSumAggregator[
            PrimaryKey,
            SecondaryKey,
            CompositeKey[
              PrimaryKey,
              SecondaryKey
            ]](
            commitInterval = commitInterval(),
            compositeKeyRangeStart = compositeKeyRangeStart,
            statsReceiver = streamsStatsReceiver,
            stateStoreName = stateStore,
            timerStoreName = timerStore.name(),
            windowSize = windowSize,
            allowedLateness = allowedLateness,
            queryableAfterClose = queryableAfterClose,
            emitOnClose = emitOnClose
          )
        ).asInstanceOf[() => Transformer[
        CompositeKeyType,
        Int,
        (TimeWindowed[PrimaryKey], WindowedValue[scala.collection.Map[SecondaryKey, Int]])
      ]] //Coerce CompositeKey[PrimaryKey, SecondaryKey] into CompositeKeyType :-/

      inner
        .transform(transformerSupplier, stateStore, timerStore.name)
    }

  }

}
