package com.twitter.finatra.kafkastreams.dsl

import com.twitter.finagle.stats.StatsReceiver
import com.twitter.finatra.kafka.serde.ScalaSerdes
import com.twitter.finatra.kafkastreams.KafkaStreamsTwitterServer
import com.twitter.finatra.kafkastreams.config.DefaultTopicConfig
import com.twitter.finatra.kafkastreams.config.FinatraTransformerFlags
import com.twitter.finatra.kafkastreams.internal.utils.sampling.IndexedSampleKeySerde
import com.twitter.finatra.kafkastreams.internal.utils.sampling.ReservoirSamplingTransformer
import com.twitter.finatra.kafkastreams.transformer.FinatraTransformer
import com.twitter.finatra.kafkastreams.transformer.utils.SamplingUtils
import com.twitter.util.Duration
import com.twitter.util.logging.Logging
import org.apache.kafka.common.serialization.Serde
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.scala.kstream.{KStream => KStreamS}
import org.apache.kafka.streams.state.internals.FinatraStores
import scala.reflect.ClassTag

/**
 * This trait adds reservoir sampling DSL methods to the Kafka Streams DSL
 */
trait FinatraDslSampling
    extends KafkaStreamsTwitterServer
    with FinatraTransformerFlags
    with Logging {

  protected def streamsStatsReceiver: StatsReceiver

  protected def kafkaStreamsBuilder: StreamsBuilder

  implicit class SamplingKeyValueStream[K: ClassTag, V](inner: KStreamS[K, V]) {

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
     * @param toSampleKey      returns the key of the sample
     * @param toSampleValue    returns the type that you want to sample
     * @param sampleSize       the size of the sample
     * @param expirationTime   the amount of time after creation that a sample should be expired
     * @param sampleName       the name of the sample
     * @param sampleKeySerde   the serde for the SampleKey
     * @param sampleValueSerde the serde for the SampleValue
     * @tparam SampleValue the type of the SampleValue
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
        FinatraStores
          .keyValueStoreBuilder(
            streamsStatsReceiver,
            FinatraStores.persistentKeyValueStore(countStoreName),
            sampleKeySerde,
            ScalaSerdes.Long
          )
          .withLoggingEnabled(DefaultTopicConfig.FinatraChangelogConfig)
      )

      val sampleStoreName = SamplingUtils.getSampleStoreName(sampleName)
      kafkaStreamsBuilder.addStateStore(
        FinatraStores
          .keyValueStoreBuilder(
            streamsStatsReceiver,
            FinatraStores.persistentKeyValueStore(sampleStoreName),
            new IndexedSampleKeySerde(sampleKeySerde),
            sampleValueSerde
          )
          .withLoggingEnabled(DefaultTopicConfig.FinatraChangelogConfig)
      )

      val timerStoreName = SamplingUtils.getTimerStoreName(sampleName)
      kafkaStreamsBuilder.addStateStore(
        FinatraTransformer.timerStore(timerStoreName, sampleKeySerde, streamsStatsReceiver))

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
        timerStoreName)
    }
  }
}
