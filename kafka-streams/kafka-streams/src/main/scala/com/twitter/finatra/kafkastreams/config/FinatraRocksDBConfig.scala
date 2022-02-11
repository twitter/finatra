package com.twitter.finatra.kafkastreams.config

import com.twitter.conversions.StorageUnitOps._
import com.twitter.finagle.stats.{LoadedStatsReceiver, StatsReceiver}
import com.twitter.finatra.kafkastreams.internal.stats.RocksDBStatsCallback
import com.twitter.inject.Injector
import com.twitter.jvm.numProcs
import com.twitter.util.logging.Logging
import com.twitter.util.StorageUnit
import java.util
import org.apache.kafka.streams.state.RocksDBConfigSetter
import org.rocksdb.{
  BlockBasedTableConfig,
  BloomFilter,
  ColumnFamilyOptionsInterface,
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
  val RocksDbBlockCacheSizeConfigDefault: StorageUnit = 200.megabytes
  val RocksDbBlockCacheSizeConfigDoc =
    """Size of the rocksdb block cache per task. We recommend that this should be about 1/3 of
      |your total memory budget. The remaining free memory can be left for the OS page cache""".stripMargin

  val RocksDbBlockCacheShardBitsConfig = "rocksdb.block.cache.shard.bits"
  val RocksDbBlockCacheShardBitsConfigDefault: Int = 1
  val RocksDbBlockCacheShardBitsConfigDoc =
    """Cache is is sharded 2^bits shards by hash of the key. Setting the value to -1 will
      |cause auto determine the size with starting size of 512KB. Shard bits will not exceed 6.
      |If mutex locking is frequent and database size is smaller then RAM, increasing this value
      |will improve locking as more shards will be available.
    """.stripMargin

  val RocksDbLZ4Config = "rocksdb.lz4"
  val RocksDbLZ4ConfigDefault: Boolean = false
  val RocksDbLZ4ConfigDoc =
    "Enable RocksDB LZ4 compression. (See https://github.com/facebook/rocksdb/wiki/Compression)"

  val RocksDbEnableStatistics = "rocksdb.statistics"
  val RocksDbEnableStatisticsDefault: Boolean = false
  val RocksDbEnableStatisticsDoc =
    """Enable RocksDB statistics. Note: RocksDB Statistics could add 5-10% degradation in performance
      |(See https://github.com/facebook/rocksdb/wiki/Statistics)""".stripMargin

  val RocksDbStatCollectionPeriodMs = "rocksdb.statistics.collection.period.ms"
  val RocksDbStatCollectionPeriodMsDefault: Int = 60000
  val RocksDbStatCollectionPeriodMsDoc = "Set the period in milliseconds for stats collection."

  val RocksDbInfoLogLevel = "rocksdb.log.info.level"
  val RocksDbInfoLogLevelDefault = "DEBUG_LEVEL"
  val RocksDbInfoLogLevelDoc =
    """Level of logging for rocksdb LOG file.
      |DEBUG_LEVEL, INFO_LEVEL, WARN_LEVEL, ERROR_LEVEL, FATAL_LEVEL, HEADER_LEVEL""".stripMargin

  val RocksDbMaxLogFileSize = "rocksdb.log.max.file.size"
  val RocksDbMaxLogFileSizeDefault: StorageUnit = 50.megabytes
  val RocksDbMaxLogFileSizeDoc =
    s"""Specify the maximal size of the info log file. If the log file is larger then
       |"rocksdb.log.keep.file.num" a new log file will be created.""".stripMargin

  val RocksDbKeepLogFileNum = "rocksdb.log.keep.file.num"
  val RocksDbKeepLogFileNumDefault: Int = 10
  val RocksDbKeepLogFileNumDoc = "Maximal info log files to be kept."

  val RocksDbCacheIndexAndFilterBlocks = "rocksdb.cache.index.and.filter.blocks"
  val RocksDbCacheIndexAndFilterBlocksDefault: Boolean = true
  val RocksDbCacheIndexAndFilterBlocksDoc =
    """Store index and filter blocks into the block cache. This bounds the memory usage,
      | which is desirable when running in a container.
      |(See https://github.com/facebook/rocksdb/wiki/Memory-usage-in-RocksDB#indexes-and-filter-blocks)""".stripMargin

  val RocksDbCachePinL0IndexAndFilterBlocks = "rocksdb.cache.pin.l0.index.and.filter.blocks"
  val RocksDbCachePinL0IndexAndFilterBlocksDefault: Boolean = true
  val RocksDbCachePinL0IndexAndFilterBlocksDoc =
    """Pin level-0 file's index and filter blocks in block cache, to avoid them from being evicted.
      | This setting is generally recommended to be turned on along to minimize the negative
      | performance impact resulted by turning on RocksDbCacheIndexAndFilterBlocks.
      |(See https://github.com/facebook/rocksdb/wiki/Block-Cache#caching-index-and-filter-blocks)""".stripMargin

  val RocksDbTableConfigBlockSize = "rocksdb.tableconfig.block.size"
  val RocksDbTableConfigBlockSizeDefault: StorageUnit = (16 * 1024).bytes
  val RocksDbTableConfigBlockSizeDoc =
    s"""Approximate size of user data packed per block. This is the uncompressed size and on disk
      |size will differ due to compression. Increasing block_size decreases memory usage and space
      |amplification, but increases read amplification.""".stripMargin

  val RocksDbTableConfigBoomFilterKeyBits = "rocksdb.tableconfig.bloomfilter.key.bits"
  val RocksDbTableConfigBoomFilterKeyBitsDefault: Int = 10
  val RocksDbTableConfigBoomFilterKeyBitsDoc =
    """
      |Bits per key in bloom filter. A bits_per_key if 10, yields a filter with ~ 1% false positive
      |rate.
      |(See https://github.com/facebook/rocksdb/wiki/RocksDB-Tuning-Guide#bloom-filters)""".stripMargin

  val RocksDbTableConfigBoomFilterMode = "rocksdb.tableconfig.bloomfilter.mode"
  val RocksDbTableConfigBoomFilterModeDefault: Boolean = true
  val RocksDbTableConfigBoomFilterModeDoc =
    s"""Toggle the mode of the bloom filer between Block-based filter (true) and Full filter (false).
      |Block-based filter is a filter for each block where Full filter is a filter per file.
      |If multiple keys are contained in the same file, the Block-based filter will serve best.
      |If keys are same among database files then Full filter is best.""".stripMargin

  val RocksDbDatabaseWriteBufferSize = "rocksdb.db.write.buffer.size"
  val RocksDbDatabaseWriteBufferSizeDefault: StorageUnit = 0.bytes
  val RocksDbDatabaseWriteBufferSizeDoc =
    """Data stored in memtables across all column families before writing to disk. Disabled by
      |specifying a 0 value, can be enabled by setting positive value in bytes. This value can be
      |used to control the total memtable sizes.""".stripMargin

  val RocksDbWriteBufferSize = "rocksdb.write.buffer.size"
  val RocksDbWriteBufferSizeDefault: StorageUnit = 1.gigabyte
  val RocksDbWriteBufferSizeDoc =
    """Data stored in memory (stored in unsorted log on disk) before writing tto sorted on-disk
      |file. Larger values will increase performance, especially on bulk loads up to
      |max_write_buffer_number write buffers available. This value can be used to adjust the control
      |of memory usage. Larger write buffers will cause longer recovery on file open.""".stripMargin

  val RocksDbManifestPreallocationSize = "rocksdb.manifest.preallocation.size"
  val RocksDbManifestPreallocationSizeDefault: StorageUnit = 4.megabytes
  val RocksDbManifestPreallocationSizeDoc =
    """Number of bytes to preallocate (via fallocate) the manifest files.
      |Default is 4mb, which is reasonable to reduce random IO as well as prevent overallocation
      |for mounts that preallocate large amounts of data (such as xfs's allocsize option).""".stripMargin

  val RocksDbMinWriteBufferNumberToMerge = "rocksdb.min.write.buffer.num.merge"
  val RocksDbMinWriteBufferNumberToMergeDefault: Int = 1
  val RocksDbMinWriteBufferNumberToMergeDoc =
    """Minimum number of write buffers that will be merged together before flushing to storage.
      |Setting of 1 will cause L0 flushed as individual files and increase read amplification
      |as all files will be scanned.""".stripMargin

  val RocksDbMaxWriteBufferNumber = "rocksdb.max.write.buffer.num"
  val RocksDbMaxWriteBufferNumberDefault: Int = 2
  val RocksDbMaxWriteBufferNumberDoc =
    """Maximum number of write buffers that will be stored in memory. While 1 buffer is flushed to disk
      |other buffers can be written.""".stripMargin

  val RocksDbBytesPerSync = "rocksdb.bytes.per.sync"
  val RocksDbBytesPerSyncDefault: StorageUnit = 1048576.bytes
  val RocksDbBytesPerSyncDoc =
    "Setting for OS to sync files to disk in the background while they are written."

  val RocksDbMaxBackgroundCompactions = "rocksdb.max.background.compactions"
  val RocksDbMaxBackgroundCompactionsDefault: Int = 4
  val RocksDbMaxBackgroundCompactionsDoc =
    """Maximum background compactions, increased values will fully utilize CPU and storage for
      |compaction routines. If stats indication higher latency due to compaction, this value could
      |be adjusted.
      |(https://github.com/facebook/rocksdb/wiki/RocksDB-Tuning-Guide#parallelism-options)""".stripMargin

  val RocksDbMaxBackgroundFlushes = "rocksdb.max.background.flushes"
  val RocksDbMaxBackgroundFlushesDefault: Int = 2
  val RocksDbMaxBackgroundFlushesDoc =
    """Maximum number of concurrent background flushes.
      |(https://github.com/facebook/rocksdb/wiki/RocksDB-Tuning-Guide#parallelism-options)
    """.stripMargin

  val RocksDbIncreaseParallelism = "rocksdb.parallelism"
  def RocksDbIncreaseParallelismDefault(): Int = numProcs().toInt
  val RocksDbIncreaseParallelismDoc =
    """Increases the total number of threads used for flushes and compaction. If rocks seems to be
      |an indication of bottleneck, this is a value you want to increase for addressing that.""".stripMargin

  val RocksDbInplaceUpdateSupport = "rocksdb.inplace.update.support"
  val RocksDbInplaceUpdateSupportDefault: Boolean = true
  val RocksDbInplaceUpdateSupportDoc =
    """Enables thread safe updates in place. If true point-in-time consistency using snapshot/iterator
      |will not be possible. Set this to true if not using snapshot iterators, otherwise false.""".stripMargin

  val RocksDbAllowConcurrentMemtableWrite = "rocksdb.allow.concurrent.memtable.write"
  val RocksDbAllowConcurrentMemtableWriteDefault: Boolean = false
  val RocksDbAllowConcurrentMemtableWriteDoc =
    """Set true if multiple writers to modify memtables in parallel. This flag is not compatible
      |with inplace update support or filter deletes, default should be false unless memtable used
      |supports it.""".stripMargin

  val RocksDbEnableWriteThreadAdaptiveYield = "rocksdb.enable.write.thread.adaptive.yield"
  val RocksDbEnableWriteThreadAdaptiveYieldDefault: Boolean = false
  val RocksDbEnableWriteThreadAdaptiveYieldDoc =
    """Set true to enable thread synchronizing with write batch group leader. Concurrent workloads
      |can be improved by setting to true.""".stripMargin

  val RocksDbCompactionStyle = "rocksdb.compaction.style"
  val RocksDbCompactionStyleDefault = "UNIVERSAL"
  val RocksDbCompactionStyleDoc =
    """Set compaction style for database.
      |UNIVERSAL, LEVEL, FIFO.""".stripMargin

  val RocksDbCompactionStyleOptimize = "rocksdb.compaction.style.optimize"
  val RocksDbCompactionStyleOptimizeDefault = true
  val RocksDbCompactionStyleOptimizeDoc =
    s"""Heavy workloads and big datasets are not the default mode of operation for rocksdb databases.
      |Enabling optimization will use rocksdb internal configuration for a range of values calculated.
      |The values calculated are based on the flag value "rocksdb.compaction.style.memtable.budget"
      |for memory given to optimize performance. Generally this should be true but means other settings
      |values might be different from values specified on the commandline.
      |(See https://github.com/facebook/rocksdb/blob/master/options/options.cc)""".stripMargin

  val RocksDbMaxBytesForLevelBase = "rocksdb.max.bytes.for.level.base"
  val RocksDbMaxBytesForLevelBaseDefault: StorageUnit = 1.gigabyte
  val RocksDbMaxBytesForLevelBaseDoc =
    """Total size of level 1, should be about the same size as level 0. Lowering this value
      |can help control memory usage.""".stripMargin

  val RocksDbLevelCompactionDynamicLevelBytes = "rocksdb.level.compaction.dynamic.level.bytes"
  val RocksDbLevelCompactionDynamicLevelBytesDefault: Boolean = true
  val RocksDbLevelCompactionDynamicLevelBytesDoc =
    """If true, enables rockdb to pick target size for each level dynamically.""".stripMargin

  val RocksDbCompactionStyleMemtableBudget = "rocksdb.compaction.style.memtable.budget"
  val RocksDbCompactionStyleMemtableBudgetDefault: StorageUnit =
    ColumnFamilyOptionsInterface.DEFAULT_COMPACTION_MEMTABLE_MEMORY_BUDGET.bytes
  val RocksDbCompactionStyleMemtableBudgetDoc =
    s"""Memory budget in bytes used when "rocksdb.compaction.style.optimize" is true."""

  // BlockCache to be shared by all RocksDB instances created on this instance.
  // Note: That a single Kafka Streams instance may get multiple tasks assigned to it
  // and each stateful task will have a separate RocksDB instance created.
  // This cache will be shared across all the tasks.
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
    setTableConfiguration(options, configs)
    setWriteBufferConfiguration(options, configs)
    setOperatingSystemProcessConfiguration(options, configs)
    setDatabaseConcurrency(options, configs)
    setCompactionConfiguration(options, configs)
    setCompression(options, configs)
    setInformationLoggingLevel(options, configs)
    setStatisticsOptions(options, configs)
  }

  private def setWriteBufferConfiguration(
    options: Options,
    configs: util.Map[String, AnyRef]
  ): Unit = {
    val dbWriteBufferSize = getBytesOrDefault(
      configs,
      FinatraRocksDBConfig.RocksDbDatabaseWriteBufferSize,
      FinatraRocksDBConfig.RocksDbDatabaseWriteBufferSizeDefault)

    val writeBufferSize = getBytesOrDefault(
      configs,
      FinatraRocksDBConfig.RocksDbWriteBufferSize,
      FinatraRocksDBConfig.RocksDbWriteBufferSizeDefault)

    val minWriteBufferNumberToMerge = getIntOrDefault(
      configs,
      FinatraRocksDBConfig.RocksDbMinWriteBufferNumberToMerge,
      FinatraRocksDBConfig.RocksDbMinWriteBufferNumberToMergeDefault)

    val maxWriteBufferNumber = getIntOrDefault(
      configs,
      FinatraRocksDBConfig.RocksDbMaxWriteBufferNumber,
      FinatraRocksDBConfig.RocksDbMaxWriteBufferNumberDefault)

    val manifestPreallocationSize = getBytesOrDefault(
      configs,
      FinatraRocksDBConfig.RocksDbManifestPreallocationSize,
      FinatraRocksDBConfig.RocksDbManifestPreallocationSizeDefault)

    options
      .setDbWriteBufferSize(dbWriteBufferSize)
      .setWriteBufferSize(writeBufferSize)
      .setMinWriteBufferNumberToMerge(minWriteBufferNumberToMerge)
      .setMaxWriteBufferNumber(maxWriteBufferNumber)
      .setManifestPreallocationSize(manifestPreallocationSize)
  }

  private def setTableConfiguration(options: Options, configs: util.Map[String, AnyRef]): Unit = {
    if (FinatraRocksDBConfig.SharedBlockCache == null) {
      val blockCacheSize =
        getBytesOrDefault(
          configs,
          FinatraRocksDBConfig.RocksDbBlockCacheSizeConfig,
          FinatraRocksDBConfig.RocksDbBlockCacheSizeConfigDefault)
      val numShardBits = getIntOrDefault(
        configs,
        FinatraRocksDBConfig.RocksDbBlockCacheShardBitsConfig,
        FinatraRocksDBConfig.RocksDbBlockCacheShardBitsConfigDefault)
      FinatraRocksDBConfig.SharedBlockCache = new LRUCache(blockCacheSize, numShardBits)
    }

    val tableConfig = new BlockBasedTableConfig

    val blockSize = getBytesOrDefault(
      configs,
      FinatraRocksDBConfig.RocksDbTableConfigBlockSize,
      FinatraRocksDBConfig.RocksDbTableConfigBlockSizeDefault)

    val bitsPerKey = getIntOrDefault(
      configs,
      FinatraRocksDBConfig.RocksDbTableConfigBoomFilterKeyBits,
      FinatraRocksDBConfig.RocksDbTableConfigBoomFilterKeyBitsDefault)

    val useBlockBasedMode = getBooleanOrDefault(
      configs,
      FinatraRocksDBConfig.RocksDbTableConfigBoomFilterMode,
      FinatraRocksDBConfig.RocksDbTableConfigBoomFilterModeDefault)

    val cacheIndexAndFilterBlocks = getBooleanOrDefault(
      configs,
      FinatraRocksDBConfig.RocksDbCacheIndexAndFilterBlocks,
      FinatraRocksDBConfig.RocksDbCacheIndexAndFilterBlocksDefault)

    val cachePinL0IndexAndFilterBlocks = getBooleanOrDefault(
      configs,
      FinatraRocksDBConfig.RocksDbCachePinL0IndexAndFilterBlocks,
      FinatraRocksDBConfig.RocksDbCachePinL0IndexAndFilterBlocksDefault)

    tableConfig.setBlockSize(blockSize)
    tableConfig.setBlockCache(FinatraRocksDBConfig.SharedBlockCache)
    tableConfig.setFilter(new BloomFilter(bitsPerKey, useBlockBasedMode))
    tableConfig.setCacheIndexAndFilterBlocks(cacheIndexAndFilterBlocks)
    tableConfig.setPinL0FilterAndIndexBlocksInCache(cachePinL0IndexAndFilterBlocks)

    options
      .setTableFormatConfig(tableConfig)
  }

  private def setOperatingSystemProcessConfiguration(
    options: Options,
    configs: util.Map[String, AnyRef]
  ): Unit = {

    val bytesPerSync = getBytesOrDefault(
      configs,
      FinatraRocksDBConfig.RocksDbBytesPerSync,
      FinatraRocksDBConfig.RocksDbBytesPerSyncDefault)

    val maxBackgroundCompactions = getIntOrDefault(
      configs,
      FinatraRocksDBConfig.RocksDbMaxBackgroundCompactions,
      FinatraRocksDBConfig.RocksDbMaxBackgroundCompactionsDefault)

    val maxBackgroundFlushes = getIntOrDefault(
      configs,
      FinatraRocksDBConfig.RocksDbMaxBackgroundFlushes,
      FinatraRocksDBConfig.RocksDbMaxBackgroundFlushesDefault)

    val increaseParallelism = getIntOrDefault(
      configs,
      FinatraRocksDBConfig.RocksDbIncreaseParallelism,
      FinatraRocksDBConfig.RocksDbIncreaseParallelismDefault())

    options
      .setBytesPerSync(bytesPerSync)
      .setMaxBackgroundCompactions(maxBackgroundCompactions)
      .setMaxBackgroundFlushes(maxBackgroundFlushes)
      .setIncreaseParallelism(Math.max(increaseParallelism, 2))
  }

  private def setDatabaseConcurrency(options: Options, configs: util.Map[String, AnyRef]): Unit = {
    val inplaceUpdateSupport = getBooleanOrDefault(
      configs,
      FinatraRocksDBConfig.RocksDbInplaceUpdateSupport,
      FinatraRocksDBConfig.RocksDbInplaceUpdateSupportDefault)

    val allowConcurrentMemtableWrite = getBooleanOrDefault(
      configs,
      FinatraRocksDBConfig.RocksDbAllowConcurrentMemtableWrite,
      FinatraRocksDBConfig.RocksDbAllowConcurrentMemtableWriteDefault)

    val enableWriteThreadAdaptiveYield = getBooleanOrDefault(
      configs,
      FinatraRocksDBConfig.RocksDbEnableWriteThreadAdaptiveYield,
      FinatraRocksDBConfig.RocksDbEnableWriteThreadAdaptiveYieldDefault)

    options
      .setInplaceUpdateSupport(inplaceUpdateSupport)
      .setAllowConcurrentMemtableWrite(allowConcurrentMemtableWrite)
      .setEnableWriteThreadAdaptiveYield(enableWriteThreadAdaptiveYield)
  }

  private def setCompactionConfiguration(
    options: Options,
    configs: util.Map[String, AnyRef]
  ): Unit = {
    val compactionStyle = CompactionStyle.valueOf(
      getStringOrDefault(
        configs,
        FinatraRocksDBConfig.RocksDbCompactionStyle,
        FinatraRocksDBConfig.RocksDbCompactionStyleDefault).toUpperCase)

    val compactionStyleOptimize = getBooleanOrDefault(
      configs,
      FinatraRocksDBConfig.RocksDbCompactionStyleOptimize,
      FinatraRocksDBConfig.RocksDbCompactionStyleOptimizeDefault)

    val maxBytesForLevelBase = getBytesOrDefault(
      configs,
      FinatraRocksDBConfig.RocksDbMaxBytesForLevelBase,
      FinatraRocksDBConfig.RocksDbMaxBytesForLevelBaseDefault)

    val levelCompactionDynamicLevelBytes = getBooleanOrDefault(
      configs,
      FinatraRocksDBConfig.RocksDbLevelCompactionDynamicLevelBytes,
      FinatraRocksDBConfig.RocksDbLevelCompactionDynamicLevelBytesDefault)

    val optimizeWithMemtableMemoryBudget = getBytesOrDefault(
      configs,
      FinatraRocksDBConfig.RocksDbCompactionStyleMemtableBudget,
      FinatraRocksDBConfig.RocksDbCompactionStyleMemtableBudgetDefault)

    options
      .setCompactionStyle(compactionStyle)
      .setMaxBytesForLevelBase(maxBytesForLevelBase)
      .setLevelCompactionDynamicLevelBytes(levelCompactionDynamicLevelBytes)

    compactionStyle match {
      case CompactionStyle.UNIVERSAL if compactionStyleOptimize =>
        options
          .optimizeUniversalStyleCompaction(optimizeWithMemtableMemoryBudget)
      case CompactionStyle.LEVEL if compactionStyleOptimize =>
        options
          .optimizeLevelStyleCompaction(optimizeWithMemtableMemoryBudget)
      case _ =>
    }
  }

  private def setCompression(options: Options, configs: util.Map[String, AnyRef]): Unit = {
    val lz4Config = getBooleanOrDefault(
      configs,
      FinatraRocksDBConfig.RocksDbLZ4Config,
      FinatraRocksDBConfig.RocksDbLZ4ConfigDefault
    )

    if (lz4Config) {
      options.setCompressionType(CompressionType.LZ4_COMPRESSION)
    }
  }

  private def setInformationLoggingLevel(
    options: Options,
    configs: util.Map[String, AnyRef]
  ): Unit = {
    val infoLogLevel = InfoLogLevel.valueOf(
      getStringOrDefault(
        configs,
        FinatraRocksDBConfig.RocksDbInfoLogLevel,
        FinatraRocksDBConfig.RocksDbInfoLogLevelDefault).toUpperCase)

    options
      .setInfoLogLevel(infoLogLevel)

  }

  private def setStatisticsOptions(options: Options, configs: util.Map[String, AnyRef]): Unit = {
    val maxLogFileSize = getBytesOrDefault(
      configs,
      FinatraRocksDBConfig.RocksDbMaxLogFileSize,
      FinatraRocksDBConfig.RocksDbMaxLogFileSizeDefault)

    options.setMaxLogFileSize(maxLogFileSize)

    val keepLogFileNum = getIntOrDefault(
      configs,
      FinatraRocksDBConfig.RocksDbKeepLogFileNum,
      FinatraRocksDBConfig.RocksDbKeepLogFileNumDefault)
    options.setKeepLogFileNum(keepLogFileNum)

    val enableStatistics = getBooleanOrDefault(
      configs,
      FinatraRocksDBConfig.RocksDbEnableStatistics,
      FinatraRocksDBConfig.RocksDbEnableStatisticsDefault)

    if (enableStatistics) {
      val statistics = new Statistics

      val statsCallback = new RocksDBStatsCallback(FinatraRocksDBConfig.globalStatsReceiver)
      val statsCollectorInput = new StatsCollectorInput(statistics, statsCallback)
      val statsCollector = new StatisticsCollector(
        util.Arrays.asList(statsCollectorInput),
        getIntOrDefault(
          configs,
          FinatraRocksDBConfig.RocksDbStatCollectionPeriodMs,
          FinatraRocksDBConfig.RocksDbStatCollectionPeriodMsDefault)
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

  private def getStringOrDefault(
    configs: util.Map[String, AnyRef],
    key: String,
    default: String
  ): String = {
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
