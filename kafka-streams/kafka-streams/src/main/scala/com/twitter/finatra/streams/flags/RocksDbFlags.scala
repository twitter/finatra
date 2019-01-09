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
}
