package com.twitter.finatra.kafkastreams.partitioning.internal

import com.twitter.finatra.streams.queryable.thrift.partitioning.StaticServiceShardPartitioner
import com.twitter.inject.Logging
import java.util
import java.util.UUID
import org.apache.kafka.common.TopicPartition
import org.apache.kafka.streams.processor.TaskId
import org.apache.kafka.streams.processor.internals.OverridableStreamsPartitionAssignor
import org.apache.kafka.streams.processor.internals.assignment.{ClientState, TaskAssignor}
import scala.collection.JavaConverters._

object StaticPartitioningStreamAssignor {
  val StreamsPreRestoreConfig = "streams.prerestore"
  val ApplicationNumInstances = "application.num.instances"
}

class StaticPartitioningStreamAssignor extends OverridableStreamsPartitionAssignor with Logging {

  private var _configs: util.Map[String, _] = _

  override def configure(configs: util.Map[String, _]): Unit = {
    super.configure(configs)
    _configs = configs
  }

  override protected def createTaskAssignor(
    partitionsForTask: util.Map[TaskId, util.Set[TopicPartition]],
    states: util.Map[UUID, ClientState],
    clients: util.Map[UUID, OverridableStreamsPartitionAssignor.ClientMetadata]
  ): TaskAssignor[UUID, TaskId] = {
    val clientStateAndHostInfo: Map[UUID, ClientStateAndHostInfo[UUID]] =
      (for ((id, metadata) <- clients.asScala) yield {
        id -> ClientStateAndHostInfo(id, metadata.state, metadata.hostInfo)
      }).toMap

    val numInstances = _configs
      .get(StaticPartitioningStreamAssignor.ApplicationNumInstances).toString.toInt //Required flag

    new StaticTaskAssignor[UUID](
      serviceShardPartitioner = new StaticServiceShardPartitioner(numShards = numInstances),
      clientsMetadata = clientStateAndHostInfo,
      taskIds = partitionsForTask.keySet().asScala.toSet
    )
  }
}
