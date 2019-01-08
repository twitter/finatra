package com.twitter.finatra.streams.queryable.thrift

import com.twitter.app.Flag
import com.twitter.finatra.streams.partitioning.StaticPartitioning
import com.twitter.finatra.streams.query.{
  QueryableFinatraCompositeWindowStore,
  QueryableFinatraKeyValueStore,
  QueryableFinatraWindowStore
}
import com.twitter.util.Duration
import org.apache.kafka.common.serialization.Serde

trait QueryableState extends StaticPartitioning {
  protected val currentShard: Flag[Int] = flag[Int]("kafka.current.shard", "")
  protected val numQueryablePartitions: Flag[Int] = flag[Int]("kafka.num.queryable.partitions", "")

  protected def queryableFinatraKeyValueStore[PK, K, V](
    storeName: String,
    primaryKeySerde: Serde[PK]
  ): QueryableFinatraKeyValueStore[PK, K, V] = {
    new QueryableFinatraKeyValueStore[PK, K, V](
      storeName,
      primaryKeySerde,
      numApplicationInstances(),
      numQueryablePartitions(),
      currentShard())
  }

  protected def queryableFinatraWindowStore[K, V](
    storeName: String,
    windowSize: Duration,
    primaryKeySerde: Serde[K]
  ): QueryableFinatraWindowStore[K, V] = {
    new QueryableFinatraWindowStore[K, V](
      storeName,
      windowSize = windowSize,
      keySerde = primaryKeySerde,
      numShards = numApplicationInstances(),
      numQueryablePartitions = numQueryablePartitions(),
      currentShardId = currentShard())
  }

  @deprecated("Use queryableFinatraWindowStore without a windowSize", "1/7/2019")
  protected def queryableFinatraWindowStore[K, V](
    storeName: String,
    primaryKeySerde: Serde[K]
  ): QueryableFinatraWindowStore[K, V] = {
    queryableFinatraWindowStore(storeName, null, primaryKeySerde)
  }

  protected def queryableFinatraCompositeWindowStore[PK, SK, V](
    storeName: String,
    windowSize: Duration,
    primaryKeySerde: Serde[PK]
  ): QueryableFinatraCompositeWindowStore[PK, SK, V] = {
    new QueryableFinatraCompositeWindowStore[PK, SK, V](
      storeName,
      windowSize = windowSize,
      primaryKeySerde = primaryKeySerde,
      numShards = numApplicationInstances(),
      numQueryablePartitions = numQueryablePartitions(),
      currentShardId = currentShard())
  }
}
