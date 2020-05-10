package com.twitter.finatra.kafkastreams.integration.sampling

import com.twitter.conversions.DurationOps._
import com.twitter.conversions.StorageUnitOps._
import com.twitter.finatra.kafka.serde.ScalaSerdes
import com.twitter.finatra.kafkastreams.KafkaStreamsTwitterServer
import com.twitter.finatra.kafkastreams.config.FinatraRocksDBConfig.{
  RocksDbBlockCacheSizeConfig,
  RocksDbEnableStatistics,
  RocksDbLZ4Config
}
import com.twitter.finatra.kafkastreams.config.FinatraTransformerFlags.{
  AutoWatermarkInterval,
  EmitWatermarkPerMessage
}
import com.twitter.finatra.kafkastreams.config.{
  FinatraRocksDBConfig,
  KafkaStreamsConfig,
  RocksDbFlags
}
import com.twitter.finatra.kafkastreams.dsl.FinatraDslSampling
import com.twitter.finatra.kafkastreams.integration.sampling.SamplingServer._
import org.apache.kafka.common.record.CompressionType
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.kstream._

object SamplingServer {
  val tweetToImpressingUserTopic = "tweet-id-to-impressing-user-id"
  val sampleName = "TweetImpressors"
}

class SamplingServer extends KafkaStreamsTwitterServer with RocksDbFlags with FinatraDslSampling {

  override def configureKafkaStreams(streamsBuilder: StreamsBuilder): Unit = {
    streamsBuilder.asScala
      .stream(topic = tweetToImpressingUserTopic)(Consumed
        .`with`(ScalaSerdes.Long, ScalaSerdes.Long))
      .sample(
        toSampleKey = (tweetId, _) => tweetId,
        toSampleValue = (_, impressorId) => impressorId,
        sampleSize = 5,
        expirationTime = Some(1.minute),
        sampleName = sampleName,
        sampleKeySerde = ScalaSerdes.Long,
        sampleValueSerde = ScalaSerdes.Long
      )
  }

  override def streamsProperties(config: KafkaStreamsConfig): KafkaStreamsConfig = {
    super
      .streamsProperties(config)
      .retries(60)
      .retryBackoff(1.second)
      .rocksDbConfigSetter[FinatraRocksDBConfig]
      .withConfig(RocksDbBlockCacheSizeConfig, rocksDbCountsStoreBlockCacheSize())
      .withConfig(RocksDbEnableStatistics, rocksDbEnableStatistics().toString)
      .withConfig(RocksDbLZ4Config, rocksDbEnableLZ4().toString)
      .withConfig(AutoWatermarkInterval, autoWatermarkIntervalFlag().toString)
      .withConfig(EmitWatermarkPerMessage, emitWatermarkPerMessageFlag().toString)
      .consumer.sessionTimeout(10.seconds)
      .consumer.heartbeatInterval(1.second)
      .producer.retries(300)
      .producer.retryBackoff(1.second)
      .producer.requestTimeout(2.minutes)
      .producer.transactionTimeout(2.minutes)
      .producer.compressionType(CompressionType.LZ4)
      .producer.batchSize(500.kilobytes)
      .producer.bufferMemorySize(256.megabytes)
      .producer.linger(10.seconds)
  }
}
