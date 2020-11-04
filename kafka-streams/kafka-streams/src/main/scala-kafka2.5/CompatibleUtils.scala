package com.twitter.finatra.kafkastreams.internal.utils

import org.apache.kafka.streams.Topology
import org.apache.kafka.streams.processor.{ProcessorContext, StateStore}
import org.apache.kafka.streams.processor.internals.InternalTopologyBuilder
import org.apache.kafka.streams.state.internals.WrappedStateStore

object CompatibleUtils {

  private val internalTopologyBuilderField =
    ReflectionUtils.getFinalField(classOf[Topology], "internalTopologyBuilder")

  private[kafkastreams] def isStateless(topology: Topology): Boolean = {
    val internalTopologyBuilder = getInternalTopologyBuilder(topology)

    internalTopologyBuilder.stateStores.isEmpty
  }

  private def getInternalTopologyBuilder(topology: Topology): InternalTopologyBuilder = {
    internalTopologyBuilderField
      .get(topology)
      .asInstanceOf[InternalTopologyBuilder]
  }

  implicit class ProcessorContextWrapper(context: ProcessorContext) {
    def unwrap[S <: StateStore, K, V](name: String): S = {
      context
        .getStateStore(name).asInstanceOf[WrappedStateStore[S, K, V]]
        .wrapped()
    }
  }

  private[kafkastreams] def resetStreamThreadId() = {}
}
