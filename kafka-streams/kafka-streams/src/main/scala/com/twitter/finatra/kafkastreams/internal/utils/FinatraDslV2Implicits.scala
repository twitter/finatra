package com.twitter.finatra.kafkastreams.internal.utils

import com.twitter.app.Flag
import com.twitter.finagle.stats.StatsReceiver
import com.twitter.finatra.kafka.serde.ScalaSerdes
import com.twitter.finatra.kafkastreams.internal.ScalaStreamsImplicits
import com.twitter.finatra.kafkastreams.internal.utils.sampling.{
  IndexedSampleKeySerde,
  ReservoirSamplingTransformer
}
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
  SamplingUtils,
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

trait FinatraDslV2Implicits extends ScalaStreamsImplicits {

  protected def kafkaStreams: KafkaStreams

  protected def streamsStatsReceiver: StatsReceiver

  protected def kafkaStreamsBuilder: StreamsBuilder

  protected def commitInterval: Flag[Duration]

  implicit class FinatraKeyValueStream[K: ClassTag, V](inner: KStreamS[K, V]) {

    /**
     * Counts and samples an attribute of a stream of records.
     *
     * This transformer uses two state stores:
     *
     * numCountsStore is a KeyValueStore[SampleKey, Long] which stores a SampleKey and the total
     * number of times that SampleKey was seen.
     *
     * sampleStore is a KeyValueStore[IndexedSampleKey[SampleKey], SampleValue] which stores the
     * samples themselves.  The Key is an IndexedSampleKey, which is your sample key wrapped with an
     * index of 0..sampleSize.  The value is the SampleValue that you want to sample.
     *
     * Example: if you had a stream of Interaction(engagingUserId, engagementType) and you wanted a
     * sample of users who performed each engagement type, then your sampleKey would be engagementType and
     * your sampleValue would be userId.
     *
     * Incoming stream:
     * (engagingUserId = 12, engagementType = Displayed)
     * (engagingUserId = 100, engagementType = Favorited)
     * (engagingUserId = 101, engagementType = Favorited)
     * (engagingUserId = 12, engagementType = Favorited)
     *
     * This is what the numCountStore table would look like:
     * Sample Key is EngagementType,
     *
     * |-----------|-------|
     * | SampleKey | Count |
     * |-----------|-------|
     * | Displayed |   1   |
     * | Favorited |   3   |
     * |-----------|-------|
     *
     *
     * This is what the sampleStore table would look like:
     * SampleKey is EngagementType
     * SampleValue is engaging user id
     *
     * |-----------------------------|-------------|
     * | IndexedSampleKey[SampleKey] | SampleValue |
     * |-----------------------------|-------------|
     * | (Displayed, index = 0)      |   12        |
     * | (Favorited, index = 0)      |   100       |
     * | (Favorited, index = 1)      |   101       |
     * | (Favorited, index = 2)      |   102       |
     * |-----------------------------|-------------|
     *
     *
     * If you want to reference the sample store(so that you can query it) the name of the store can
     * be found by calling `SamplingUtils.getSampleStoreName(sampleName`.  You can reference the
     * name of the count store by calling `SamplingUtils.getNumCountsStoreName(sampleName)`
     *
     * *Note* This method will create the state stores for you.
     *
     * @param toSampleKey returns the key of the sample
     * @param toSampleValue returns the type that you want to sample
     * @param sampleSize the size of the sample
     * @param expirationTime the amount of time after creation that a sample should be expired
     * @param sampleName the name of the sample
     * @param sampleKeySerde the serde for the SampleKey
     * @param sampleValueSerde the serde for the SampleValue
     * @tparam SampleKey the type of the SampleKey
     * @tparam SampleValue the type of the SampleVaule
     *
     * @return a stream of SampleKey and SampleValue
     */
    def sample[SampleKey: ClassTag, SampleValue: ClassTag](
      toSampleKey: (K, V) => SampleKey,
      toSampleValue: (K, V) => SampleValue,
      sampleSize: Int,
      expirationTime: Option[Duration],
      sampleName: String,
      sampleKeySerde: Serde[SampleKey],
      sampleValueSerde: Serde[SampleValue]
    ): KStreamS[SampleKey, SampleValue] = {

      val countStoreName = SamplingUtils.getNumCountsStoreName(sampleName)
      kafkaStreamsBuilder.addStateStore(
        Stores
          .keyValueStoreBuilder(
            Stores.persistentKeyValueStore(countStoreName),
            sampleKeySerde,
            ScalaSerdes.Long
          )
          .withLoggingEnabled(DefaultTopicConfig.FinatraChangelogConfig)
      )

      val sampleStoreName = SamplingUtils.getSampleStoreName(sampleName)
      kafkaStreamsBuilder.addStateStore(
        Stores
          .keyValueStoreBuilder(
            Stores.persistentKeyValueStore(sampleStoreName),
            new IndexedSampleKeySerde(sampleKeySerde),
            sampleValueSerde
          )
          .withLoggingEnabled(DefaultTopicConfig.FinatraChangelogConfig)
      )

      val timerStoreName = SamplingUtils.getTimerStoreName(sampleName)
      kafkaStreamsBuilder.addStateStore(
        FinatraTransformer.timerStore(timerStoreName, sampleKeySerde)
      )

      val transformer = () =>
        new ReservoirSamplingTransformer[K, V, SampleKey, SampleValue](
          statsReceiver = streamsStatsReceiver,
          toSampleKey = toSampleKey,
          toSampleValue = toSampleValue,
          sampleSize = sampleSize,
          expirationTime = expirationTime,
          countStoreName = countStoreName,
          sampleStoreName = sampleStoreName,
          timerStoreName = timerStoreName
        )

      inner.transform[SampleKey, SampleValue](
        transformer,
        countStoreName,
        sampleStoreName,
        timerStoreName
      )
    }
  }

  /* ---------------------------------------- */
  implicit class FinatraKStream[K: ClassTag](inner: KStreamS[K, Int]) extends Logging {

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
            TimeWindowed.windowStart(messageTime, windowSize.inMillis)
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
            windowStart = (messageTime, key, windowedValue) => windowedValue.startMs
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
