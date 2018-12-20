package com.twitter.finatra.streams.queryable.thrift.partitioning

import com.twitter.finatra.streams.queryable.thrift.client.partitioning.utils.KafkaUtils.{
  murmur2,
  toPositive
}
import com.twitter.finatra.streams.queryable.thrift.domain.{KafkaPartitionId, ServiceShardId}

object StaticServiceShardPartitioner {
  def partitionId(numPartitions: Int, keyBytes: Array[Byte]): KafkaPartitionId = {
    val partitionId = toPositive(murmur2(keyBytes)) % numPartitions
    KafkaPartitionId(partitionId)
  }
}

case class StaticServiceShardPartitioner(numShards: Int) extends ServiceShardPartitioner {

  override def activeShardId(kafkaPartitionId: KafkaPartitionId): ServiceShardId = {
    ServiceShardId(kafkaPartitionId.id % numShards)
  }

  override def standbyShardIds(kafkaPartitionId: KafkaPartitionId): Seq[ServiceShardId] = {
    Seq(standbyShardId(kafkaPartitionId))
  }

  override def shardIds(numPartitions: Int, keyBytes: Array[Byte]): IndexedSeq[ServiceShardId] = {
    val kafkaPartitionId = StaticServiceShardPartitioner.partitionId(numPartitions, keyBytes)
    IndexedSeq(activeShardId(kafkaPartitionId), standbyShardId(kafkaPartitionId))
  }

  //Note: We divide numShards by 2 so that standby tasks will be assigned on a different
  //      "side" of the available shards. This allows deploys to upgrade the first half of the
  //      shards while the second half of the shards can remain with the full set of tasks
  def standbyShardId(kafkaPartitionId: KafkaPartitionId): ServiceShardId = {
    ServiceShardId((kafkaPartitionId.id + (numShards / 2)) % numShards)
  }
}
