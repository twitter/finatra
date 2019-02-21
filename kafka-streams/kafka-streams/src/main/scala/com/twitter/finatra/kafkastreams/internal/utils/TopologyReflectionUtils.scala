package com.twitter.finatra.kafkastreams.internal.utils

import org.apache.kafka.streams.Topology
import org.apache.kafka.streams.processor.internals.InternalTopologyBuilder

private[kafkastreams] object TopologyReflectionUtils {

  private val internalTopologyBuilderField =
    ReflectionUtils.getFinalField(classOf[Topology], "internalTopologyBuilder")

  def isStateless(topology: Topology): Boolean = {
    val internalTopologyBuilder = getInternalTopologyBuilder(topology)

    internalTopologyBuilder.allStateStoreName().isEmpty &&
    internalTopologyBuilder.globalStateStores().isEmpty
  }

  private def getInternalTopologyBuilder(topology: Topology): InternalTopologyBuilder = {
    internalTopologyBuilderField
      .get(topology)
      .asInstanceOf[InternalTopologyBuilder]
  }
}
