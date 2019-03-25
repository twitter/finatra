package com.twitter.finatra.kafkastreams.integration.config

import com.twitter.finatra.kafka.serde.{UnKeyed, UnKeyedSerde}
import com.twitter.finatra.kafkastreams.KafkaStreamsTwitterServer
import com.twitter.finatra.kafkastreams.config.FinatraRocksDBConfig._
import com.twitter.finatra.kafkastreams.config.{DefaultTopicConfig, FinatraRocksDBConfig, KafkaStreamsConfig, RocksDbFlags}
import com.twitter.finatra.kafkastreams.test.{FinatraTopologyTester, TopologyFeatureTest}
import com.twitter.finatra.kafkastreams.transformer.FinatraTransformer
import com.twitter.finatra.kafkastreams.transformer.domain.Time
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.kstream.{Consumed, Produced}
import org.apache.kafka.streams.state.Stores
import org.joda.time.DateTime

class FinatraRocksDBConfigFeatureTest extends TopologyFeatureTest {
  private val appId = "no-op"
  private val stateStoreName = "test-state-store"

  private val kafkaStreamsTwitterServer: KafkaStreamsTwitterServer = new KafkaStreamsTwitterServer with RocksDbFlags {
    override val name: String = appId
    override protected def configureKafkaStreams(builder: StreamsBuilder): Unit = {
      builder.addStateStore(
        Stores
          .keyValueStoreBuilder(
            Stores.persistentKeyValueStore(stateStoreName),
            UnKeyedSerde,
            Serdes.String
          ).withLoggingEnabled(DefaultTopicConfig.FinatraChangelogConfig)
      )

      val finatraTransformerSupplier = () =>
        new FinatraTransformer[UnKeyed, String, UnKeyed, String](statsReceiver = statsReceiver) {
          override def onInit(): Unit = { }
          override def onClose(): Unit = { }
          override def onMessage(messageTime: Time, unKeyed: UnKeyed, string: String): Unit = { }
        }

      builder.asScala
        .stream("source")(Consumed.`with`(UnKeyedSerde, Serdes.String))
        .transform(finatraTransformerSupplier, stateStoreName)
        .to("sink")(Produced.`with`(UnKeyedSerde, Serdes.String))
    }

    override def streamsProperties(config: KafkaStreamsConfig): KafkaStreamsConfig = {
      super
        .streamsProperties(config)
        .rocksDbConfigSetter[FinatraRocksDBConfig]
        .withConfig(RocksDbBlockCacheSizeConfig, rocksDbCountsStoreBlockCacheSize())
        .withConfig(RocksDbBlockCacheShardBitsConfig, rocksDbBlockCacheShardBitsConfig())
        .withConfig(RocksDbLZ4Config, rocksDbEnableLZ4().toString)
        .withConfig(RocksDbEnableStatistics, rocksDbEnableStatistics().toString)
        .withConfig(RocksDbStatCollectionPeriodMs, rocksDbStatCollectionPeriodMs())
        .withConfig(RocksDbInfoLogLevel, rocksDbInfoLogLevel())
        .withConfig(RocksDbMaxLogFileSize, rocksDbMaxLogFileSize())
        .withConfig(RocksDbKeepLogFileNum, rocksDbKeepLogFileNum())
        .withConfig(RocksDbCacheIndexAndFilterBlocks, rocksDbCacheIndexAndFilterBlocks())
        .withConfig(RocksDbCachePinL0IndexAndFilterBlocks, rocksDbCachePinL0IndexAndFilterBlocks())
        .withConfig(RocksDbTableConfigBlockSize, rocksDbTableConfigBlockSize())
        .withConfig(RocksDbTableConfigBoomFilterKeyBits, rocksDbTableConfigBoomFilterKeyBits())
        .withConfig(RocksDbTableConfigBoomFilterMode, rocksDbTableConfigBoomFilterMode())
        .withConfig(RocksDbDatabaseWriteBufferSize, rocksDbDatabaseWriteBufferSize())
        .withConfig(RocksDbWriteBufferSize, rocksDbWriteBufferSize())
        .withConfig(RocksDbManifestPreallocationSize, rocksDbManifestPreallocationSize())
        .withConfig(RocksDbMinWriteBufferNumberToMerge, rocksDbMinWriteBufferNumberToMerge())
        .withConfig(RocksDbMaxWriteBufferNumber, rocksDbMaxWriteBufferNumber())
        .withConfig(RocksDbBytesPerSync, rocksDbBytesPerSync())
        .withConfig(RocksDbMaxBackgroundCompactions, rocksDbMaxBackgroundCompactions())
        .withConfig(RocksDbMaxBackgroundFlushes, rocksDbMaxBackgroundFlushes())
        .withConfig(RocksDbIncreaseParallelism, rocksDbIncreaseParallelism())
        .withConfig(RocksDbInplaceUpdateSupport, rocksDbInplaceUpdateSupport())
        .withConfig(RocksDbAllowConcurrentMemtableWrite, rocksDbAllowConcurrentMemtableWrite())
        .withConfig(RocksDbEnableWriteThreadAdaptiveYield, rocksDbEnableWriteThreadAdaptiveYield())
        .withConfig(RocksDbCompactionStyle, rocksDbCompactionStyle())
        .withConfig(RocksDbCompactionStyleOptimize, rocksDbCompactionStyleOptimize())
        .withConfig(RocksDbMaxBytesForLevelBase, rocksDbMaxBytesForLevelBase())
        .withConfig(RocksDbLevelCompactionDynamicLevelBytes, rocksDbLevelCompactionDynamicLevelBytes())
        .withConfig(RocksDbCompactionStyleMemtableBudget, rocksDbCompactionStyleMemtableBudget())
    }
  }

  private val _topologyTester = FinatraTopologyTester(
    kafkaApplicationId = appId,
    server = kafkaStreamsTwitterServer,
    startingWallClockTime = DateTime.now,
    flags = Map(
      "rocksdb.block.cache.size" -> "1.byte",
      "rocksdb.block.cache.shard.bits" -> "2",
      "rocksdb.lz4" -> "true",
      "rocksdb.statistics" -> "true",
      "rocksdb.statistics.collection.period.ms" -> "60001",
      "rocksdb.log.info.level" -> "INFO_LEVEL",
      "rocksdb.log.max.file.size" -> "2.bytes",
      "rocksdb.log.keep.file.num" -> "3",
      "rocksdb.cache.index.and.filter.blocks" -> "false",
      "rocksdb.cache.pin.l0.index.and.filter.blocks" -> "false",
      "rocksdb.tableconfig.block.size" -> "4.bytes",
      "rocksdb.tableconfig.bloomfilter.key.bits" -> "5",
      "rocksdb.tableconfig.bloomfilter.mode" -> "false",
      "rocksdb.db.write.buffer.size" -> "6.bytes",
      "rocksdb.write.buffer.size" -> "7.bytes",
      "rocksdb.manifest.preallocation.size" -> "5.megabytes",
      "rocksdb.min.write.buffer.num.merge" -> "8",
      "rocksdb.max.write.buffer.num" -> "9",
      "rocksdb.bytes.per.sync" -> "10.bytes",
      "rocksdb.max.background.compactions" -> "11",
      "rocksdb.max.background.flushes" -> "12",
      "rocksdb.parallelism" -> "2",
      "rocksdb.inplace.update.support" -> "false",
      "rocksdb.allow.concurrent.memtable.write" -> "true",
      "rocksdb.enable.write.thread.adaptive.yield" -> "true",
      "rocksdb.compaction.style" -> "UNIVERSAL",
      "rocksdb.compaction.style.optimize" -> "false",
      "rocksdb.max.bytes.for.level.base" -> "13.bytes",
      "rocksdb.level.compaction.dynamic.level.bytes" -> "false",
      "rocksdb.compaction.style.memtable.budget" -> "14.bytes"
    )
  )

  override protected def topologyTester: FinatraTopologyTester = {
    _topologyTester
  }

  override def beforeEach(): Unit = {
    super.beforeEach()
    topologyTester.reset()
    topologyTester.topic(
      "source",
      UnKeyedSerde,
      Serdes.String
    )
    topologyTester.topic(
      "sink",
      UnKeyedSerde,
      Serdes.String
    )
  }

  test("rocksdb properties") {
    val properties = topologyTester.properties
    properties.getProperty("rocksdb.config.setter") should be(
      "com.twitter.finatra.kafkastreams.config.FinatraRocksDBConfig")
    properties.getProperty("rocksdb.block.cache.size") should be("1")
    properties.getProperty("rocksdb.block.cache.shard.bits") should be("2")
    properties.getProperty("rocksdb.lz4") should be("true")
    properties.getProperty("rocksdb.statistics") should be("true")
    properties.getProperty("rocksdb.statistics.collection.period.ms") should be("60001")
    properties.getProperty("rocksdb.log.info.level") should be("INFO_LEVEL")
    properties.getProperty("rocksdb.log.max.file.size") should be("2")
    properties.getProperty("rocksdb.log.keep.file.num") should be("3")
    properties.getProperty("rocksdb.cache.index.and.filter.blocks") should be("false")
    properties.getProperty("rocksdb.cache.pin.l0.index.and.filter.blocks") should be("false")
    properties.getProperty("rocksdb.tableconfig.block.size") should be("4")
    properties.getProperty("rocksdb.tableconfig.bloomfilter.key.bits") should be("5")
    properties.getProperty("rocksdb.tableconfig.bloomfilter.mode") should be("false")
    properties.getProperty("rocksdb.db.write.buffer.size") should be("6")
    properties.getProperty("rocksdb.write.buffer.size") should be("7")
    properties.getProperty("rocksdb.manifest.preallocation.size") should be("5242880")
    properties.getProperty("rocksdb.min.write.buffer.num.merge") should be("8")
    properties.getProperty("rocksdb.max.write.buffer.num") should be("9")
    properties.getProperty("rocksdb.bytes.per.sync") should be("10")
    properties.getProperty("rocksdb.max.background.compactions") should be("11")
    properties.getProperty("rocksdb.max.background.flushes") should be("12")
    properties.getProperty("rocksdb.parallelism") should be("2")
    properties.getProperty("rocksdb.inplace.update.support") should be("false")
    properties.getProperty("rocksdb.allow.concurrent.memtable.write") should be("true")
    properties.getProperty("rocksdb.enable.write.thread.adaptive.yield") should be("true")
    properties.getProperty("rocksdb.compaction.style") should be("UNIVERSAL")
    properties.getProperty("rocksdb.compaction.style.optimize") should be("false")
    properties.getProperty("rocksdb.max.bytes.for.level.base") should be("13")
    properties.getProperty("rocksdb.level.compaction.dynamic.level.bytes") should be("false")
    properties.getProperty("rocksdb.compaction.style.memtable.budget") should be("14")

    topologyTester.driver
      .getKeyValueStore[UnKeyed, String](stateStoreName) shouldNot be(null)
  }
}
