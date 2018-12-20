package com.twitter.finatra.streams.queryable.thrift

import com.twitter.app.Flag
import com.twitter.finatra.streams.partitioning.StaticPartitioning
import com.twitter.finatra.streams.query.{
  QueryableFinatraCompositeWindowStore,
  QueryableFinatraKeyValueStore,
  QueryableFinatraWindowStore
}
import com.twitter.finatra.streams.transformer.domain.TimeWindowed
import org.apache.kafka.common.serialization.Serde
import org.apache.kafka.streams.state.QueryableStoreTypes

trait QueryableState extends StaticPartitioning {
  protected val currentShard: Flag[Int] = flag[Int]("kafka.current.shard", "")
  protected val numQueryablePartitions: Flag[Int] = flag[Int]("kafka.num.queryable.partitions", "")

  protected def queryableFinatraKeyValueStore[PK, K, V](
    storeName: String,
    primaryKeySerde: Serde[PK]
  ): QueryableFinatraKeyValueStore[PK, K, V] = {
    new QueryableFinatraKeyValueStore[PK, K, V](
      kafkaStreams.store(storeName, QueryableStoreTypes.keyValueStore[K, V]),
      primaryKeySerde,
      numApplicationInstances(),
      numQueryablePartitions(),
      currentShard()
    )
  }

  protected def queryableFinatraWindowStore[K, V](
    storeName: String,
    primaryKeySerde: Serde[K]
  ): QueryableFinatraWindowStore[K, V] = {
    new QueryableFinatraWindowStore[K, V](
      kafkaStreams.store(storeName, QueryableStoreTypes.keyValueStore[TimeWindowed[K], V]),
      primaryKeySerde,
      numApplicationInstances(),
      numQueryablePartitions(),
      currentShard()
    )
  }

  protected def queryableFinatraCompositeWindowStore[K, PK, SK, V](
    storeName: String,
    primaryKeySerde: Serde[PK]
  ): QueryableFinatraCompositeWindowStore[K, PK, SK, V] = {
    new QueryableFinatraCompositeWindowStore[K, PK, SK, V](
      kafkaStreams,
      storeName,
      primaryKeySerde,
      numApplicationInstances(),
      numQueryablePartitions(),
      currentShard()
    )
  }
}
