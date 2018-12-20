package com.twitter.finatra.streams.queryable.thrift.partitioning

import com.twitter.finatra.streams.queryable.thrift.client.partitioning.utils.KafkaUtils.{
  murmur2,
  toPositive
}
import com.twitter.finatra.streams.queryable.thrift.domain.{KafkaPartitionId, ServiceShardId}

object KafkaPartitioner {
  def partitionId(numPartitions: Int, keyBytes: Array[Byte]): KafkaPartitionId = {
    val partitionId = toPositive(murmur2(keyBytes)) % numPartitions
    KafkaPartitionId(partitionId)
  }
}

case class KafkaPartitioner(serviceShardPartitioner: ServiceShardPartitioner, numPartitions: Int) {

  def shardIds(keyBytes: Array[Byte]): IndexedSeq[ServiceShardId] = {
    val kafkaPartitionId = KafkaPartitioner.partitionId(numPartitions, keyBytes)
    IndexedSeq(
      serviceShardPartitioner.activeShardId(kafkaPartitionId),
      serviceShardPartitioner.standbyShardIds(kafkaPartitionId).head
    )
  }
}
