package com.twitter.finatra.kafkastreams.partitioners

import org.apache.kafka.streams.processor.StreamPartitioner

/**
 * Partitions in a round robin fashion going from 0 to numPartitions -1 and wrapping around again.
 *
 * @tparam K the key on the stream
 * @tparam V the value on the stream
 */
class RoundRobinStreamPartitioner[K, V] extends StreamPartitioner[K, V] {

  private var nextPartitionId: Int = 0

  override def partition(topic: String, key: K, value: V, numPartitions: Int): Integer = {
    val partitionIdToReturn = nextPartitionId
    nextPartitionId = (nextPartitionId + 1) % numPartitions
    partitionIdToReturn
  }
}
