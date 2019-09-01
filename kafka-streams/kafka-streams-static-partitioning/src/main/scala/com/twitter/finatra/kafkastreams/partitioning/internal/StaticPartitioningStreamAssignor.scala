package com.twitter.finatra.kafkastreams.partitioning.internal

import com.twitter.finatra.streams.queryable.thrift.domain.ServiceShardId
import com.twitter.finatra.streams.queryable.thrift.partitioning.StaticServiceShardPartitioner
import com.twitter.inject.Logging
import java.util
import java.util.UUID
import org.apache.kafka.common.TopicPartition
import org.apache.kafka.streams.processor.TaskId
import org.apache.kafka.streams.processor.internals.OverridableStreamsPartitionAssignor
import org.apache.kafka.streams.processor.internals.assignment.{ClientState, TaskAssignor}
import scala.collection.JavaConverters._
import scala.util.control.NonFatal

object StaticPartitioningStreamAssignor {
  val StreamsPreRestoreConfig = "streams.prerestore"
  val ApplicationNumInstances = "application.num.instances"

  def parseShardId(applicationServerHost: String): ServiceShardId = {
    val firstPeriodIndex = applicationServerHost.indexOf('.')

    val shardId = try {
      applicationServerHost.substring(0, firstPeriodIndex).toInt
    } catch {
      case NonFatal(e) =>
        throw new Exception(
          "Finatra Kafka Stream's StaticPartitioning functionality requires the " +
            "'kafka.application.server' flag value to be specified as '<kafka.current.shard>.<unused_hostname>:<unused_port>" +
            " where unused_hostname can be empty and unused_port must be > 0. As an example, to configure the server" +
            " that represents shard #5, you can set 'kafka.application.server=5.:80'. In this example, port 80 is unused and does not" +
            " need to represent an actual open port"
        )
    }

    ServiceShardId(shardId)
  }
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
