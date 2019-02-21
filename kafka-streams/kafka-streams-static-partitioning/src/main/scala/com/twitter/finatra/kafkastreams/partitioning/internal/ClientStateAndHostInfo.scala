package com.twitter.finatra.kafkastreams.partitioning.internal

import com.twitter.finatra.streams.queryable.thrift.domain.ServiceShardId
import org.apache.kafka.streams.processor.internals.assignment.ClientState
import org.apache.kafka.streams.state.HostInfo

case class ClientStateAndHostInfo[ID](id: ID, clientState: ClientState, hostInfo: HostInfo) {

  val serviceShardId: ServiceShardId = {
    StaticPartitioningStreamAssignor.parseShardId(hostInfo.host())
  }
}
