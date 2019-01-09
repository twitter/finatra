package com.twitter.finatra.streams.queryable.thrift.partitioning

import com.twitter.finatra.streams.queryable.thrift.domain.{KafkaPartitionId, ServiceShardId}

trait ServiceShardPartitioner {
  def shardIds(numPartitions: Int, keyBytes: Array[Byte]): IndexedSeq[ServiceShardId]

  def activeShardId(kafkaPartitionId: KafkaPartitionId): ServiceShardId

  def standbyShardIds(kafkaPartitionId: KafkaPartitionId): Seq[ServiceShardId]
}
