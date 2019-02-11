package com.twitter.finatra.kafkastreams.config

import com.twitter.conversions.StorageUnitOps._
import com.twitter.inject.server.TwitterServer

trait RocksDbFlags extends TwitterServer {

  protected val rocksDbCountsStoreBlockCacheSize =
    flag(
      name = FinatraRocksDBConfig.RocksDbBlockCacheSizeConfig,
      default = 200.megabytes,
      help =
        """Size of the rocksdb block cache per task. We recommend that this should be about 1/3 of
          |your total memory budget. The remaining free memory can be left for the OS page cache""".stripMargin
    )

  protected val rocksDbEnableStatistics =
    flag(
      name = FinatraRocksDBConfig.RocksDbEnableStatistics,
      default = false,
      help =
        """Enable RocksDB statistics. Note: RocksDB Statistics could add 5-10% degradation in performance
          |(See https://github.com/facebook/rocksdb/wiki/Statistics)""".stripMargin
    )

  protected val rocksDbStatCollectionPeriodMs =
    flag(
      name = FinatraRocksDBConfig.RocksDbStatCollectionPeriodMs,
      default = 60000,
      help = "Set the period in milliseconds for stats collection."
    )

  protected val rocksDbEnableLZ4 =
    flag(
      name = FinatraRocksDBConfig.RocksDbLZ4Config,
      default = false,
      help =
        "Enable RocksDB LZ4 compression. (See https://github.com/facebook/rocksdb/wiki/Compression)"
    )

  protected val rocksDbInfoLogLevel =
    flag(
      name = FinatraRocksDBConfig.RocksDbInfoLogLevel,
      default = "INFO_LEVEL",
      help =
        """Level of logging for rocksdb LOG file.
          |DEBUG_LEVEL, INFO_LEVEL, WARN_LEVEL, ERROR_LEVEL, FATAL_LEVEL, HEADER_LEVEL""".stripMargin
    )

  protected val rocksDbMaxLogFileSize =
    flag(
      name = FinatraRocksDBConfig.RocksDbMaxLogFileSize,
      default = 50.megabytes,
      help =
        s"""Specify the maximal size of the info log file. If the log file is larger then
           |${FinatraRocksDBConfig.RocksDbKeepLogFileNum} a new log file will be created.""".stripMargin
    )

  protected val rocksDbKeepLogFileNum =
    flag(
      name = FinatraRocksDBConfig.RocksDbKeepLogFileNum,
      default = 10,
      help = "Maximal info log files to be kept."
    )

  protected val rocksDbCacheIndexAndFilterBlocks =
    flag(
      name = FinatraRocksDBConfig.RocksDbCacheIndexAndFilterBlocks,
      default = true,
      help =
        """Store index and filter blocks into the block cache. This bounds the memory usage,
          | which is desirable when running in a container.
          |(See https://github.com/facebook/rocksdb/wiki/Memory-usage-in-RocksDB#indexes-and-filter-blocks)""".stripMargin
    )

  protected val rocksDbCachePinL0IndexAndFilterBlocks =
    flag(
      name = FinatraRocksDBConfig.RocksDbCachePinL0IndexAndFilterBlocks,
      default = true,
      help =
        """Pin level-0 file's index and filter blocks in block cache, to avoid them from being evicted.
          | This setting is generally recommended to be turned on along to minimize the negative
          | performance impact resulted by turning on RocksDbCacheIndexAndFilterBlocks.
          |(See https://github.com/facebook/rocksdb/wiki/Block-Cache#caching-index-and-filter-blocks)""".stripMargin
    )
}
