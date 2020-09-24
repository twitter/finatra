package com.twitter.finatra.kafkastreams.utils

import com.twitter.finatra.kafkastreams.KafkaStreamsTwitterServer
import com.twitter.finatra.kafkastreams.internal.utils.CompatibleUtils
import org.apache.kafka.streams.Topology

/**
 *
 * StatelessKafkaStreamsServer is used for stateless Kafka transformations that do not need to store data in local state stores.
 * Note 1: When using this class, server startup will fail if a local state store is used.
 * Note 2: In the future, we could potentially, use a different TaskAssignment strategy to avoid extra unneeded metadata in the
 *         partition join requests
 */
abstract class StatelessKafkaStreamsTwitterServer extends KafkaStreamsTwitterServer {

  override def createKafkaStreamsTopology(): Topology = {
    val topology = super.createKafkaStreamsTopology()
    if (!CompatibleUtils.isStateless(topology)) {
      throw new UnsupportedOperationException(
        "This server is using StatelessKafkaStreamsTwitterServer but it is not a stateless topology"
      )
    }
    topology
  }
}
