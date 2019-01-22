package com.twitter.finatra.streams.flags

import com.twitter.conversions.StorageUnitOps._
import com.twitter.finatra.kafkastreams.config.FinatraRocksDBConfig
import com.twitter.inject.server.TwitterServer

trait RocksDbFlags extends TwitterServer {

  protected val rocksDbCountsStoreBlockCacheSize =
    flag(
      name = FinatraRocksDBConfig.RocksDbBlockCacheSizeConfig,
      default = 200.megabytes,
      help =
        "Size of the rocksdb block cache per task. We recommend that this should be about 1/3 of your total memory budget. The remaining free memory can be left for the OS page cache"
    )

  protected val rocksDbEnableStatistics =
    flag(
      name = FinatraRocksDBConfig.RocksDbEnableStatistics,
      default = false,
      help =
        "Enable RocksDB statistics. Note: RocksDB Statistics could add 5-10% degradation in performance (see https://github.com/facebook/rocksdb/wiki/Statistics)"
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
}
