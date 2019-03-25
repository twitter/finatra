package com.twitter.finatra.kafkastreams.config

import com.twitter.app.Flag
import com.twitter.inject.server.TwitterServer
import com.twitter.util.StorageUnit

trait RocksDbFlags extends TwitterServer {

  protected val rocksDbCountsStoreBlockCacheSize: Flag[StorageUnit] =
    flag(
      name = FinatraRocksDBConfig.RocksDbBlockCacheSizeConfig,
      default = FinatraRocksDBConfig.RocksDbBlockCacheSizeConfigDefault,
      help = FinatraRocksDBConfig.RocksDbBlockCacheSizeConfigDoc
    )

  protected val rocksDbBlockCacheShardBitsConfig: Flag[Int] =
    flag(
      name = FinatraRocksDBConfig.RocksDbBlockCacheShardBitsConfig,
      default = FinatraRocksDBConfig.RocksDbBlockCacheShardBitsConfigDefault,
      help = FinatraRocksDBConfig.RocksDbBlockCacheShardBitsConfigDoc
    )

  protected val rocksDbEnableStatistics: Flag[Boolean] =
    flag(
      name = FinatraRocksDBConfig.RocksDbEnableStatistics,
      default = FinatraRocksDBConfig.RocksDbEnableStatisticsDefault,
      help = FinatraRocksDBConfig.RocksDbEnableStatisticsDoc
    )

  protected val rocksDbStatCollectionPeriodMs: Flag[Int] =
    flag(
      name = FinatraRocksDBConfig.RocksDbStatCollectionPeriodMs,
      default = FinatraRocksDBConfig.RocksDbStatCollectionPeriodMsDefault,
      help = FinatraRocksDBConfig.RocksDbStatCollectionPeriodMsDoc
    )

  protected val rocksDbEnableLZ4: Flag[Boolean] =
    flag(
      name = FinatraRocksDBConfig.RocksDbLZ4Config,
      default = FinatraRocksDBConfig.RocksDbLZ4ConfigDefault,
      help = FinatraRocksDBConfig.RocksDbLZ4ConfigDoc
    )

  protected val rocksDbInfoLogLevel: Flag[String] =
    flag(
      name = FinatraRocksDBConfig.RocksDbInfoLogLevel,
      default = FinatraRocksDBConfig.RocksDbInfoLogLevelDefault,
      help = FinatraRocksDBConfig.RocksDbInfoLogLevelDoc
    )

  protected val rocksDbMaxLogFileSize: Flag[StorageUnit] =
    flag(
      name = FinatraRocksDBConfig.RocksDbMaxLogFileSize,
      default = FinatraRocksDBConfig.RocksDbMaxLogFileSizeDefault,
      help = FinatraRocksDBConfig.RocksDbMaxLogFileSizeDoc
    )

  protected val rocksDbKeepLogFileNum: Flag[Int] =
    flag(
      name = FinatraRocksDBConfig.RocksDbKeepLogFileNum,
      default = FinatraRocksDBConfig.RocksDbKeepLogFileNumDefault,
      help = FinatraRocksDBConfig.RocksDbKeepLogFileNumDoc
    )

  protected val rocksDbCacheIndexAndFilterBlocks: Flag[Boolean] =
    flag(
      name = FinatraRocksDBConfig.RocksDbCacheIndexAndFilterBlocks,
      default = FinatraRocksDBConfig.RocksDbCacheIndexAndFilterBlocksDefault,
      help = FinatraRocksDBConfig.RocksDbCacheIndexAndFilterBlocksDoc
    )

  protected val rocksDbCachePinL0IndexAndFilterBlocks: Flag[Boolean] =
    flag(
      name = FinatraRocksDBConfig.RocksDbCachePinL0IndexAndFilterBlocks,
      default = FinatraRocksDBConfig.RocksDbCachePinL0IndexAndFilterBlocksDefault,
      help = FinatraRocksDBConfig.RocksDbCachePinL0IndexAndFilterBlocksDoc
    )

  protected val rocksDbTableConfigBlockSize: Flag[StorageUnit] =
    flag(
      name = FinatraRocksDBConfig.RocksDbTableConfigBlockSize,
      default = FinatraRocksDBConfig.RocksDbTableConfigBlockSizeDefault,
      help = FinatraRocksDBConfig.RocksDbTableConfigBlockSizeDoc
    )

  protected val rocksDbTableConfigBoomFilterKeyBits: Flag[Int] =
    flag(
      name = FinatraRocksDBConfig.RocksDbTableConfigBoomFilterKeyBits,
      default = FinatraRocksDBConfig.RocksDbTableConfigBoomFilterKeyBitsDefault,
      help = FinatraRocksDBConfig.RocksDbTableConfigBoomFilterKeyBitsDoc
    )

  protected val rocksDbTableConfigBoomFilterMode: Flag[Boolean] =
    flag(
      name = FinatraRocksDBConfig.RocksDbTableConfigBoomFilterMode,
      default = FinatraRocksDBConfig.RocksDbTableConfigBoomFilterModeDefault,
      help = FinatraRocksDBConfig.RocksDbTableConfigBoomFilterModeDoc
    )

  protected val rocksDbDatabaseWriteBufferSize: Flag[StorageUnit] =
    flag(
      name = FinatraRocksDBConfig.RocksDbDatabaseWriteBufferSize,
      default = FinatraRocksDBConfig.RocksDbDatabaseWriteBufferSizeDefault,
      help = FinatraRocksDBConfig.RocksDbDatabaseWriteBufferSizeDoc
    )

  protected val rocksDbWriteBufferSize: Flag[StorageUnit] =
    flag(
      name = FinatraRocksDBConfig.RocksDbWriteBufferSize,
      default = FinatraRocksDBConfig.RocksDbWriteBufferSizeDefault,
      help = FinatraRocksDBConfig.RocksDbWriteBufferSizeDoc
    )

  protected val rocksDbManifestPreallocationSize: Flag[StorageUnit] =
    flag(
      name = FinatraRocksDBConfig.RocksDbManifestPreallocationSize,
      default = FinatraRocksDBConfig.RocksDbManifestPreallocationSizeDefault,
      help = FinatraRocksDBConfig.RocksDbManifestPreallocationSizeDoc
    )

  protected val rocksDbMinWriteBufferNumberToMerge: Flag[Int] =
    flag(
      name = FinatraRocksDBConfig.RocksDbMinWriteBufferNumberToMerge,
      default = FinatraRocksDBConfig.RocksDbMinWriteBufferNumberToMergeDefault,
      help = FinatraRocksDBConfig.RocksDbMinWriteBufferNumberToMergeDoc
    )

  protected val rocksDbMaxWriteBufferNumber: Flag[Int] =
    flag(
      name = FinatraRocksDBConfig.RocksDbMaxWriteBufferNumber,
      default = FinatraRocksDBConfig.RocksDbMaxWriteBufferNumberDefault,
      help = FinatraRocksDBConfig.RocksDbMaxWriteBufferNumberDoc
    )

  protected val rocksDbBytesPerSync: Flag[StorageUnit] =
    flag(
      name = FinatraRocksDBConfig.RocksDbBytesPerSync,
      default = FinatraRocksDBConfig.RocksDbBytesPerSyncDefault,
      help = FinatraRocksDBConfig.RocksDbBytesPerSyncDoc
    )

  protected val rocksDbMaxBackgroundCompactions: Flag[Int] =
    flag(
      name = FinatraRocksDBConfig.RocksDbMaxBackgroundCompactions,
      default = FinatraRocksDBConfig.RocksDbMaxBackgroundCompactionsDefault,
      help = FinatraRocksDBConfig.RocksDbMaxBackgroundCompactionsDoc
    )

  protected val rocksDbMaxBackgroundFlushes: Flag[Int] =
    flag(
      name = FinatraRocksDBConfig.RocksDbMaxBackgroundFlushes,
      default = FinatraRocksDBConfig.RocksDbMaxBackgroundFlushesDefault,
      help = FinatraRocksDBConfig.RocksDbMaxBackgroundFlushesDoc
    )

  protected val rocksDbIncreaseParallelism: Flag[Int] =
    flag(
      name = FinatraRocksDBConfig.RocksDbIncreaseParallelism,
      default = FinatraRocksDBConfig.RocksDbIncreaseParallelismDefault(),
      help = FinatraRocksDBConfig.RocksDbIncreaseParallelismDoc
    )

  protected val rocksDbInplaceUpdateSupport: Flag[Boolean] =
    flag(
      name = FinatraRocksDBConfig.RocksDbInplaceUpdateSupport,
      default = FinatraRocksDBConfig.RocksDbInplaceUpdateSupportDefault,
      help = FinatraRocksDBConfig.RocksDbInplaceUpdateSupportDoc
    )

  protected val rocksDbAllowConcurrentMemtableWrite: Flag[Boolean] =
    flag(
      name = FinatraRocksDBConfig.RocksDbAllowConcurrentMemtableWrite,
      default = FinatraRocksDBConfig.RocksDbAllowConcurrentMemtableWriteDefault,
      help = FinatraRocksDBConfig.RocksDbAllowConcurrentMemtableWriteDoc
    )

  protected val rocksDbEnableWriteThreadAdaptiveYield: Flag[Boolean] =
    flag(
      name = FinatraRocksDBConfig.RocksDbEnableWriteThreadAdaptiveYield,
      default = FinatraRocksDBConfig.RocksDbEnableWriteThreadAdaptiveYieldDefault,
      help = FinatraRocksDBConfig.RocksDbEnableWriteThreadAdaptiveYieldDoc
    )

  protected val rocksDbCompactionStyle: Flag[String] =
    flag(
      name = FinatraRocksDBConfig.RocksDbCompactionStyle,
      default = FinatraRocksDBConfig.RocksDbCompactionStyleDefault,
      help = FinatraRocksDBConfig.RocksDbCompactionStyleDoc
    )

  protected val rocksDbCompactionStyleOptimize: Flag[Boolean] =
    flag(
      name = FinatraRocksDBConfig.RocksDbCompactionStyleOptimize,
      default = FinatraRocksDBConfig.RocksDbCompactionStyleOptimizeDefault,
      help = FinatraRocksDBConfig.RocksDbCompactionStyleOptimizeDoc
    )

  protected val rocksDbMaxBytesForLevelBase: Flag[StorageUnit] =
    flag(
      name = FinatraRocksDBConfig.RocksDbMaxBytesForLevelBase,
      default = FinatraRocksDBConfig.RocksDbMaxBytesForLevelBaseDefault,
      help = FinatraRocksDBConfig.RocksDbMaxBytesForLevelBaseDoc
    )

  protected val rocksDbLevelCompactionDynamicLevelBytes: Flag[Boolean] =
    flag(
      name = FinatraRocksDBConfig.RocksDbLevelCompactionDynamicLevelBytes,
      default = FinatraRocksDBConfig.RocksDbLevelCompactionDynamicLevelBytesDefault,
      help = FinatraRocksDBConfig.RocksDbLevelCompactionDynamicLevelBytesDoc
    )

  protected val rocksDbCompactionStyleMemtableBudget: Flag[StorageUnit] =
    flag(
      name = FinatraRocksDBConfig.RocksDbCompactionStyleMemtableBudget,
      default = FinatraRocksDBConfig.RocksDbCompactionStyleMemtableBudgetDefault,
      help = FinatraRocksDBConfig.RocksDbCompactionStyleMemtableBudgetDoc
    )
}
