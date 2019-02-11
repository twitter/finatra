package com.twitter.finatra.kafkastreams.config

import com.twitter.conversions.StorageUnitOps._
import com.twitter.finagle.stats.{LoadedStatsReceiver, StatsReceiver}
import com.twitter.finatra.kafkastreams.internal.stats.RocksDBStatsCallback
import com.twitter.inject.{Injector, Logging}
import com.twitter.jvm.numProcs
import com.twitter.util.StorageUnit
import java.util
import org.apache.kafka.streams.state.RocksDBConfigSetter
import org.rocksdb.{
  BlockBasedTableConfig,
  BloomFilter,
  CompactionStyle,
  CompressionType,
  InfoLogLevel,
  LRUCache,
  Options,
  Statistics,
  StatisticsCollector,
  StatsCollectorInput,
  StatsLevel
}

object FinatraRocksDBConfig {

  val RocksDbBlockCacheSizeConfig = "rocksdb.block.cache.size"
  val RocksDbBlockCacheShardBitsConfig = "rocksdb.block.cache.shard.bits"
  val RocksDbLZ4Config = "rocksdb.lz4"
  val RocksDbEnableStatistics = "rocksdb.statistics"
  val RocksDbStatCollectionPeriodMs = "rocksdb.statistics.collection.period.ms"
  val RocksDbInfoLogLevel = "rocksdb.log.info.level"
  val RocksDbMaxLogFileSize = "rocksdb.log.max.file.size"
  val RocksDbKeepLogFileNum = "rocksdb.log.keep.file.num"
  val RocksDbCacheIndexAndFilterBlocks = "rocksdb.cache.index.and.filter.blocks"
  val RocksDbCachePinL0IndexAndFilterBlocks = "rocksdb.cache.pin.l0.index.and.filter.blocks"

  // BlockCache to be shared by all RocksDB instances created on this instance. Note: That a single Kafka Streams instance may get multiple tasks assigned to it
  // and each stateful task will have a separate RocksDB instance created. This cache will be shared across all the tasks.
  // See: https://github.com/facebook/rocksdb/wiki/Block-Cache
  private var SharedBlockCache: LRUCache = _

  private var globalStatsReceiver: StatsReceiver = LoadedStatsReceiver

  def init(injector: Injector): Unit = {
    globalStatsReceiver = injector.instance[StatsReceiver]
  }
}

/**
 * Maintains the RocksDB configuration used by Kafka Streams.
 */
class FinatraRocksDBConfig extends RocksDBConfigSetter with Logging {

  //See https://github.com/facebook/rocksdb/wiki/Setup-Options-and-Basic-Tuning#other-general-options
  override def setConfig(
    storeName: String,
    options: Options,
    configs: util.Map[String, AnyRef]
  ): Unit = {
    if (FinatraRocksDBConfig.SharedBlockCache == null) {
      val blockCacheSize =
        getBytesOrDefault(configs, FinatraRocksDBConfig.RocksDbBlockCacheSizeConfig, 100.megabytes)
      val numShardBits = getIntOrDefault(configs, FinatraRocksDBConfig.RocksDbBlockCacheShardBitsConfig, 1)
      FinatraRocksDBConfig.SharedBlockCache = new LRUCache(blockCacheSize, numShardBits)
    }

    val tableConfig = new BlockBasedTableConfig
    tableConfig.setBlockSize(16 * 1024)
    tableConfig.setBlockCache(FinatraRocksDBConfig.SharedBlockCache)
    tableConfig.setFilter(new BloomFilter(10))
    tableConfig.setCacheIndexAndFilterBlocks(
      getBooleanOrDefault(configs, FinatraRocksDBConfig.RocksDbCacheIndexAndFilterBlocks, true))

    tableConfig.setPinL0FilterAndIndexBlocksInCache(
      getBooleanOrDefault(configs, FinatraRocksDBConfig.RocksDbCachePinL0IndexAndFilterBlocks, true))

    options
      .setTableFormatConfig(tableConfig)

    options
      .setDbWriteBufferSize(0)
      .setWriteBufferSize(1.gigabyte.inBytes) //TODO: Make configurable with default value equal to RocksDB default (which is much lower than 1 GB!)
      .setMinWriteBufferNumberToMerge(1)
      .setMaxWriteBufferNumber(2)

    options
      .setBytesPerSync(1048576) //See: https://github.com/facebook/rocksdb/wiki/Setup-Options-and-Basic-Tuning#other-general-options
      .setMaxBackgroundCompactions(4)
      .setMaxBackgroundFlushes(2)
      .setIncreaseParallelism(Math.max(numProcs().toInt, 2))

    /* From the docs: "Allows thread-safe inplace updates. If this is true, there is no way to
        achieve point-in-time consistency using snapshot or iterator (assuming concurrent updates).
        Hence iterator and multi-get will return results which are not consistent as of any point-in-time." */
    options
      .setInplaceUpdateSupport(true) //We set to true since we never have concurrent updates
      .setAllowConcurrentMemtableWrite(false)
      .setEnableWriteThreadAdaptiveYield(false)

    options
      .setCompactionStyle(CompactionStyle.UNIVERSAL)
      .setMaxBytesForLevelBase(1.gigabyte.inBytes)
      .setLevelCompactionDynamicLevelBytes(true)
      .optimizeUniversalStyleCompaction()

    if (configs.get(FinatraRocksDBConfig.RocksDbLZ4Config) == "true") {
      options.setCompressionType(CompressionType.LZ4_COMPRESSION)
    }

    val infoLogLevel = InfoLogLevel.valueOf(getStringOrDefault(
      configs,
      FinatraRocksDBConfig.RocksDbInfoLogLevel, "DEBUG_LEVEL").toUpperCase)
    options
      .setInfoLogLevel(infoLogLevel)

    val maxLogFileSize =
      getBytesOrDefault(configs, FinatraRocksDBConfig.RocksDbMaxLogFileSize, 50.megabytes)
    options.setMaxLogFileSize(maxLogFileSize)

    val keepLogFileNum =
      getIntOrDefault(configs, FinatraRocksDBConfig.RocksDbKeepLogFileNum, 10)
    options.setKeepLogFileNum(keepLogFileNum)

    if (configs.get(FinatraRocksDBConfig.RocksDbEnableStatistics) == "true") {
      val statistics = new Statistics

      val statsCallback = new RocksDBStatsCallback(FinatraRocksDBConfig.globalStatsReceiver)
      val statsCollectorInput = new StatsCollectorInput(statistics, statsCallback)
      val statsCollector = new StatisticsCollector(
        util.Arrays.asList(statsCollectorInput),
        getIntOrDefault(configs, FinatraRocksDBConfig.RocksDbStatCollectionPeriodMs, 60000)
      )
      statsCollector.start()

      statistics.setStatsLevel(StatsLevel.ALL)
      options
        .setStatistics(statistics)
        .setStatsDumpPeriodSec(20)
    }
  }

  private def getBytesOrDefault(
    configs: util.Map[String, AnyRef],
    key: String,
    default: StorageUnit
  ): Long = {
    val valueBytesString = configs.get(key)
    if (valueBytesString != null) {
      valueBytesString.toString.toLong
    } else {
      default.inBytes
    }
  }

  private def getIntOrDefault(configs: util.Map[String, AnyRef], key: String, default: Int): Int = {
    val valueString = configs.get(key)
    if (valueString != null) {
      valueString.toString.toInt
    } else {
      default
    }
  }

  private def getStringOrDefault(configs: util.Map[String, AnyRef], key: String, default: String): String = {
    val valueString = configs.get(key)
    if (valueString != null) {
      valueString.toString
    } else {
      default
    }
  }

  private def getBooleanOrDefault(
    configs: util.Map[String, AnyRef],
    key: String,
    default: Boolean
  ): Boolean = {
    val valueString = configs.get(key)
    if (valueString != null) {
      valueString.toString.toBoolean
    } else {
      default
    }
  }
}
