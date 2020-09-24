package com.twitter.finatra.kafkastreams.internal.utils

import java.util.concurrent.atomic.AtomicInteger
import org.apache.kafka.streams.Topology
import org.apache.kafka.streams.processor.{ProcessorContext, StateStore}
import org.apache.kafka.streams.processor.internals.InternalTopologyBuilder
import org.apache.kafka.streams.processor.internals.StreamThread
import org.apache.kafka.streams.state.internals.WrappedStateStore

private[kafkastreams] object CompatibleUtils {

  private val internalTopologyBuilderField =
    ReflectionUtils.getFinalField(classOf[Topology], "internalTopologyBuilder")

  def isStateless(topology: Topology): Boolean = {
    val internalTopologyBuilder = getInternalTopologyBuilder(topology)

    internalTopologyBuilder.getStateStores.isEmpty
  }

  private def getInternalTopologyBuilder(topology: Topology): InternalTopologyBuilder = {
    internalTopologyBuilderField
      .get(topology)
      .asInstanceOf[InternalTopologyBuilder]
  }

  def getUnwrappedStateStore[K, V](name: String, processorContext: ProcessorContext): StateStore = {
    val unwrappedStateStore = processorContext
      .getStateStore(name).asInstanceOf[WrappedStateStore[_]]
      .wrapped()

    unwrappedStateStore
  }

  def resetStreamThreadId() = {
    val streamThreadClass = classOf[StreamThread]
    val streamThreadIdSequenceField = streamThreadClass
      .getDeclaredField("STREAM_THREAD_ID_SEQUENCE")
    streamThreadIdSequenceField.setAccessible(true)
    val streamThreadIdSequence = streamThreadIdSequenceField.get(null).asInstanceOf[AtomicInteger]
    streamThreadIdSequence.set(1)
  }
}
