package com.twitter.finatra.kafkastreams.dsl

import com.twitter.app.Flag
import com.twitter.conversions.StorageUnitOps._
import com.twitter.conversions.DurationOps._
import com.twitter.finagle.stats.StatsReceiver
import com.twitter.finatra.kafka.serde.ScalaSerdes
import com.twitter.finatra.kafkastreams.config.FinatraTransformerFlags
import com.twitter.finatra.kafkastreams.flushing.FlushingAwareServer
import com.twitter.finatra.kafkastreams.transformer.FinatraTransformer
import com.twitter.finatra.kafkastreams.transformer.aggregation.AggregatorTransformer
import com.twitter.finatra.kafkastreams.transformer.aggregation.FixedTimeWindowedSerde
import com.twitter.finatra.kafkastreams.transformer.aggregation.TimeWindowed
import com.twitter.finatra.kafkastreams.transformer.aggregation.WindowedValue
import com.twitter.finatra.kafkastreams.transformer.domain.Time
import com.twitter.finatra.kafkastreams.utils.ScalaStreamsImplicits
import com.twitter.util.Duration
import com.twitter.util.logging.Logging
import org.apache.kafka.common.config.TopicConfig.CLEANUP_POLICY_COMPACT
import org.apache.kafka.common.config.TopicConfig.CLEANUP_POLICY_CONFIG
import org.apache.kafka.common.config.TopicConfig.CLEANUP_POLICY_DELETE
import org.apache.kafka.common.config.TopicConfig.DELETE_RETENTION_MS_CONFIG
import org.apache.kafka.common.config.TopicConfig.RETENTION_MS_CONFIG
import org.apache.kafka.common.config.TopicConfig.SEGMENT_BYTES_CONFIG
import org.apache.kafka.common.serialization.Serde
import org.apache.kafka.streams.scala.kstream.{KStream => KStreamS}
import org.apache.kafka.streams.state.internals.FinatraStores
import org.apache.kafka.streams.KafkaStreams
import org.apache.kafka.streams.StreamsBuilder
import scala.collection.JavaConverters._
import scala.reflect.ClassTag

/**
 * This trait adds Enhanced Windowed Aggregation DSL methods which offer additional control that
 * is not included in the default Kafka Streams DSL
 *
 * Note: We extend FlushingAwareServer, because WindowStore flags are used by the AggregatorTransformer
 * which requires us to be a "Flushing Aware" server. We plan to improve this coupling in the future.
 */
trait FinatraDslWindowedAggregations
    extends FlushingAwareServer
    with FinatraTransformerFlags
    with ScalaStreamsImplicits
    with Logging {

  protected val windowSize = flag("window.size", 1.hour, "Window size")
  protected val emitOnClose = flag("emit.on.close", false, "Emit records on window close")
  protected val emitUpdatedEntriesOnCommit =
    flag("emit.updated.entries.on.commit", false, "Emit updated entries on commit interval")

  protected val queryableAfterClose = flag(
    "queryable.after.close",
    0.minutes,
    "Time for window entries to remain queryable after the window closes")

  protected val allowedLateness = flag("allowed.lateness", 5.minutes, "Allowed lateness")

  protected def kafkaStreams: KafkaStreams

  protected def streamsStatsReceiver: StatsReceiver

  protected def kafkaStreamsBuilder: StreamsBuilder

  protected def commitInterval: Flag[Duration]

  implicit class WindowedAggregationsKeyValueStream[K: ClassTag, V](inner: KStreamS[K, V]) {

    /**
     * For each unique key, aggregate the values in the stream that occurred within a given time window
     * and store those values in a StateStore named stateStore.
     *
     * *Note* This method will create the state stores for you.
     *
     * A TimeWindow is a tumbling window of fixed length defined by the windowSize parameter.
     *
     * A Window is closed after event time passes the end of a TimeWindow + allowedLateness.
     *
     * After a window is closed, if emitOnClose=true it is forwarded out of this transformer with a
     * [[WindowedValue.windowResultType]] of [[com.twitter.finatra.kafkastreams.transformer.aggregation.WindowClosed]]
     *
     * If a record arrives after a window is closed it is immediately forwarded out of this
     * transformer with a [[WindowedValue.windowResultType]] of [[com.twitter.finatra.kafkastreams.transformer.aggregation.Restatement]]
     *
     * @param stateStore the name of the StateStore used to maintain the counts.
     * @param windowSize splits the stream of data into buckets of data of windowSize,
     *                   based on the timestamp of each message.
     * @param allowedLateness allow messages that are up to this amount late to be added to the
     *                        store, otherwise they are emitted as restatements.
     * @param queryableAfterClose allow state to be queried up to this amount after the window is closed.
     * @param keySerde Serde for the keys in the StateStore.
     * @param aggregateSerde Serde for the aggregation type
     * @param initializer Initializer function that computes an initial intermediate aggregation result
     * @param aggregator Aggregator function that computes a new aggregate result
     * @param windowStart Function to determine the window start time given the message time, key,
     *                    and value. If not set, the default window start is calculated using the
     *                    message time and the window size.
     * @param emitOnClose Emit messages for each entry in the window when the window close. Emitted
     *                    entries will have a WindowResultType set to WindowClosed.
     * @param emitUpdatedEntriesOnCommit Emit messages for each updated entry in the window on the Kafka
     *                                   Streams commit interval. Emitted entries will have a
     *                                   WindowResultType set to WindowOpen.
     * @param windowSizeRetentionMultiplier A multiplier on top of the windowSize to ensure data is
     *                                      not deleted from the changelog prematurely. Allows for clock drift. Default is 2
     *
     * @return a stream of Keys for a particular timewindow, and the aggregation of the values for that key
     *         within a particular timewindow.
     */
    def aggregate[Aggregate](
      stateStore: String,
      windowSize: Duration,
      allowedLateness: Duration,
      queryableAfterClose: Duration,
      keySerde: Serde[K],
      aggregateSerde: Serde[Aggregate],
      initializer: () => Aggregate,
      aggregator: ((K, V), Aggregate) => Aggregate,
      windowStart: (Time, K, V) => Time = null,
      emitOnClose: Boolean = true,
      emitUpdatedEntriesOnCommit: Boolean = false,
      windowSizeRetentionMultiplier: Int = 2
    ): KStreamS[TimeWindowed[K], WindowedValue[Aggregate]] = {
      val windowedKeySerde = FixedTimeWindowedSerde(inner = keySerde, duration = windowSize)

      val aggregateStore = FinatraStores
        .keyValueStoreBuilder(
          statsReceiver = streamsStatsReceiver,
          supplier = FinatraStores.persistentKeyValueStore(stateStore),
          keySerde = windowedKeySerde,
          valueSerde = aggregateSerde)
        .withCachingEnabled()
        .withLoggingEnabled(Map(
          CLEANUP_POLICY_CONFIG -> (CLEANUP_POLICY_COMPACT + ", " + CLEANUP_POLICY_DELETE),
          SEGMENT_BYTES_CONFIG -> 100.megabytes.inBytes.toString,
          RETENTION_MS_CONFIG -> (windowSizeRetentionMultiplier * windowSize.inMillis).toString,
          //configure delete retention such that standby replicas have 5 minutes to read deletes
          DELETE_RETENTION_MS_CONFIG -> 5.minutes.inMillis.toString
        ).asJava)

      val timerStore = FinatraTransformer.timerStore(
        name = s"$stateStore-TimerStore",
        timerKeySerde = ScalaSerdes.Long,
        statsReceiver = streamsStatsReceiver)

      debug(s"Add $aggregateStore")
      kafkaStreamsBuilder.addStateStore(aggregateStore)

      debug(s"Add $timerStore")
      kafkaStreamsBuilder.addStateStore(timerStore)

      val transformerSupplier = () =>
        new AggregatorTransformer[K, V, Aggregate](
          commitInterval = commitInterval(),
          statsReceiver = streamsStatsReceiver,
          stateStoreName = stateStore,
          timerStoreName = timerStore.name,
          windowSize = windowSize,
          allowedLateness = allowedLateness,
          queryableAfterClose = queryableAfterClose,
          initializer = initializer,
          aggregator = aggregator,
          customWindowStart = windowStart,
          emitOnClose = emitOnClose,
          emitUpdatedEntriesOnCommit = emitUpdatedEntriesOnCommit)

      inner.transform(transformerSupplier, stateStore, timerStore.name)
    }
  }

  /* ---------------------------------------- */
  implicit class FinatraKStream[K: ClassTag](inner: KStreamS[K, Int]) extends Logging {

    /**
     * For each unique key, sum the values in the stream that occurred within a given time window
     * and store those values in a StateStore named stateStore.
     *
     * *Note* This method will create the state stores for you.
     *
     * A TimeWindow is a tumbling window of fixed length defined by the windowSize parameter.
     *
     * A Window is closed after event time passes the end of a TimeWindow + allowedLateness.
     *
     * After a window is closed, if emitOnClose=true it is forwarded out of this transformer with a
     * [[WindowedValue.windowResultType]] of [[com.twitter.finatra.kafkastreams.transformer.aggregation.WindowClosed]]
     *
     * If a record arrives after a window is closed it is immediately forwarded out of this
     * transformer with a [[WindowedValue.windowResultType]] of [[com.twitter.finatra.kafkastreams.transformer.aggregation.Restatement]]
     *
     * @param stateStore the name of the StateStore used to maintain the counts.
     * @param windowSize splits the stream of data into buckets of data of windowSize,
     *                   based on the timestamp of each message.
     * @param allowedLateness allow messages that are up to this amount late to be added to the
     *                        store, otherwise they are emitted as restatements.
     * @param queryableAfterClose allow state to be queried up to this amount after the window is closed.
     * @param keySerde Serde for the keys in the StateStore.
     * @param emitOnClose Emit messages for each entry in the window when the window close. Emitted
     *                    entries will have a WindowResultType set to WindowClosed.
     * @param emitUpdatedEntriesOnCommit Emit messages for each updated entry in the window on the Kafka
     *                                   Streams commit interval. Emitted entries will have a
     *                                   WindowResultType set to WindowOpen.
     * @param windowSizeRetentionMultiplier A multiplier on top of the windowSize to ensure data is not deleted from the changelog prematurely. Allows for clock drift. Default is 2
     *
     * @return a stream of Keys for a particular timewindow, and the sum of the values for that key
     *         within a particular timewindow.
     */
    def sum(
      stateStore: String,
      windowSize: Duration,
      allowedLateness: Duration,
      queryableAfterClose: Duration,
      keySerde: Serde[K],
      emitUpdatedEntriesOnCommit: Boolean = false,
      emitOnClose: Boolean = true,
      windowSizeRetentionMultiplier: Int = 2
    ): KStreamS[TimeWindowed[K], WindowedValue[Int]] = {
      inner.aggregate(
        stateStore = stateStore,
        windowSize = windowSize,
        allowedLateness = allowedLateness,
        queryableAfterClose = queryableAfterClose,
        keySerde = keySerde,
        aggregateSerde = ScalaSerdes.Int,
        initializer = () => 0,
        aggregator = {
          case ((key: K, incrCount: Int), aggregateCount: Int) =>
            aggregateCount + incrCount
        },
        emitOnClose = emitOnClose,
        emitUpdatedEntriesOnCommit = emitUpdatedEntriesOnCommit,
        windowSizeRetentionMultiplier = windowSizeRetentionMultiplier
      )
    }
  }

  /* ---------------------------------------- */
  implicit class FinatraKeyToWindowedValueStream[
    K: ClassTag,
    TimeWindowedType <: TimeWindowed[Int]
  ](
    inner: KStreamS[K, TimeWindowedType])
      extends Logging {

    /**
     * For each unique key, sum the TimeWindowed values in the stream that occurred within the
     * TimeWindowed window and store those values in a StateStore named stateStore.
     *
     * *Note* This method will create the state stores for you.
     *
     * A TimeWindow is a tumbling window of fixed length defined by the windowSize parameter.
     *
     * A Window is closed after event time passes the end of a TimeWindow + allowedLateness.
     *
     * After a window is closed, if emitOnClose=true it is forwarded out of this transformer with a
     * [[WindowedValue.windowResultType]] of [[com.twitter.finatra.kafkastreams.transformer.aggregation.WindowClosed]]
     *
     * If a record arrives after a window is closed it is immediately forwarded out of this
     * transformer with a [[WindowedValue.windowResultType]] of [[com.twitter.finatra.kafkastreams.transformer.aggregation.Restatement]]
     *
     * @param stateStore the name of the StateStore used to maintain the counts.
     * @param windowSize splits the stream of data into buckets of data of windowSize,
     *                   based on the timestamp of each message.
     * @param allowedLateness allow messages that are up to this amount late to be added to the
     *                        store, otherwise they are emitted as restatements.
     * @param queryableAfterClose allow state to be queried up to this amount after the window is closed.
     * @param keySerde Serde for the keys in the StateStore.
     * @param emitOnClose Emit messages for each entry in the window when the window close. Emitted
     *                    entries will have a WindowResultType set to WindowClosed.
     * @param emitUpdatedEntriesOnCommit Emit messages for each updated entry in the window on the Kafka
     *                                   Streams commit interval. Emitted entries will have a
     *                                   WindowResultType set to WindowOpen.
     * @param windowSizeRetentionMultiplier A multiplier on top of the windowSize to ensure data is not deleted from the changelog prematurely. Allows for clock drift. Default is 2
     *
     * @return a stream of Keys for a particular timewindow, and the sum of the values for that key
     *         within a particular timewindow.
     */
    def sum(
      stateStore: String,
      allowedLateness: Duration,
      queryableAfterClose: Duration,
      emitOnClose: Boolean,
      windowSize: Duration,
      keySerde: Serde[K],
      emitUpdatedEntriesOnCommit: Boolean = false,
      windowSizeRetentionMultiplier: Int = 2
    ): KStreamS[TimeWindowed[K], WindowedValue[Int]] = {
      val windowSizeMillis = windowSize.inMillis

      WindowedAggregationsKeyValueStream(inner).aggregate(
        stateStore = stateStore,
        windowSize = windowSize,
        allowedLateness = allowedLateness,
        queryableAfterClose = queryableAfterClose,
        keySerde = keySerde,
        aggregateSerde = ScalaSerdes.Int,
        initializer = () => 0,
        aggregator = {
          case ((key: K, windowedIncrCount: TimeWindowed[Int]), aggregateCount: Int) =>
            aggregateCount + windowedIncrCount.value
        },
        windowStart = {
          case (time, key, timeWindowedCount) =>
            assert(timeWindowedCount.sizeMillis == windowSizeMillis)
            timeWindowedCount.start
        },
        emitOnClose = emitOnClose,
        emitUpdatedEntriesOnCommit = emitUpdatedEntriesOnCommit,
        windowSizeRetentionMultiplier = windowSizeRetentionMultiplier
      )
    }
  }
}
