package com.twitter.finatra.kafkastreams.internal.utils

import org.apache.kafka.streams.Topology
import org.apache.kafka.streams.processor.{ProcessorContext, StateStore}
import org.apache.kafka.streams.processor.internals.InternalTopologyBuilder
import org.apache.kafka.streams.state.internals.WrappedStateStore

private[kafkastreams] object CompatibleUtils {

  private val internalTopologyBuilderField =
    ReflectionUtils.getFinalField(classOf[Topology], "internalTopologyBuilder")

  def isStateless(topology: Topology): Boolean = {
    val internalTopologyBuilder = getInternalTopologyBuilder(topology)

    internalTopologyBuilder.stateStores.isEmpty
  }

  private def getInternalTopologyBuilder(topology: Topology): InternalTopologyBuilder = {
    internalTopologyBuilderField
      .get(topology)
      .asInstanceOf[InternalTopologyBuilder]
  }

  def getUnwrappedStateStore[K, V](name: String, processorContext: ProcessorContext): StateStore = {
    val unwrappedStateStore = processorContext
      .getStateStore(name).asInstanceOf[WrappedStateStore[_, K, V]]
      .wrapped().asInstanceOf[StateStore]

    unwrappedStateStore
  }

  def resetStreamThreadId() = {}
}
