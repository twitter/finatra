package com.twitter.finatra.kafkastreams.internal.utils

import java.util.concurrent.atomic.AtomicInteger
import org.apache.kafka.streams.Topology
import org.apache.kafka.streams.processor.{ProcessorContext, StateStore}
import org.apache.kafka.streams.processor.internals.InternalTopologyBuilder
import org.apache.kafka.streams.processor.internals.StreamThread
import org.apache.kafka.streams.state.internals.WrappedStateStore

object CompatibleUtils {

  private val internalTopologyBuilderField =
    ReflectionUtils.getFinalField(classOf[Topology], "internalTopologyBuilder")

  private[kafkastreams] def isStateless(topology: Topology): Boolean = {
    val internalTopologyBuilder = getInternalTopologyBuilder(topology)

    internalTopologyBuilder.getStateStores.isEmpty
  }

  private def getInternalTopologyBuilder(topology: Topology): InternalTopologyBuilder = {
    internalTopologyBuilderField
      .get(topology)
      .asInstanceOf[InternalTopologyBuilder]
  }

  private[kafkastreams] def resetStreamThreadId() = {
    val streamThreadClass = classOf[StreamThread]
    val streamThreadIdSequenceField = streamThreadClass
      .getDeclaredField("STREAM_THREAD_ID_SEQUENCE")
    streamThreadIdSequenceField.setAccessible(true)
    val streamThreadIdSequence = streamThreadIdSequenceField.get(null).asInstanceOf[AtomicInteger]
    streamThreadIdSequence.set(1)
  }

  implicit class ProcessorContextWrapper(context: ProcessorContext) {
    def unwrap[S <: StateStore, K, V](name: String): S = {
      context
        .getStateStore(name).asInstanceOf[WrappedStateStore[S]]
        .wrapped()
    }
  }
}
