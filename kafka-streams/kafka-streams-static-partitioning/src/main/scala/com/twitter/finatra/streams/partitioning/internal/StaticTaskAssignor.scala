package com.twitter.finatra.streams.partitioning.internal

import com.twitter.finagle.stats.LoadedStatsReceiver
import com.twitter.finatra.streams.partitioning.StaticPartitioning
import com.twitter.finatra.streams.queryable.thrift.domain.{
  KafkaGroupId,
  KafkaPartitionId,
  ServiceShardId
}
import com.twitter.finatra.streams.queryable.thrift.partitioning.ServiceShardPartitioner
import com.twitter.inject.Logging
import org.apache.kafka.streams.processor.TaskId
import org.apache.kafka.streams.processor.internals.assignment.TaskAssignor
import org.apache.kafka.streams.state.HostInfo
import scala.collection.mutable
import scala.language.existentials

/**
 * StaticTaskAssignor is a TaskAssignor that statically/deterministically assigns tasks to Kafka Streams instances/shards
 * Deterministic assignments has the following benefits over the dynamic default StickyTaskAssignor:
 * * Since the same active and standby tasks are assigned to the same shards, monitoring and alerting is easier to reason about
 * * PreRestore functionality is easier to implement because we know where the active and standby tasks live
 * * Queryable State is easier to implement since the query client can deterministically know where the active and standby tasks are available
 *   to be queried
 *
 * TODO: Detect zombie shards (e.g. multiple hosts with the same instanceId)
 * TODO: Spread out standby replicas so they fall across multiple instances (currently, when a single instance restarts, all it's tasks go to the same instance containing all it's standby replicas)
 * TODO: Currently the "interleaving" code in StreamPartitionAssignor spreads out active and standby tasks across multiple threads. To speed up active threads, we may want to move all standby processing to it's own thread)...
 */
class StaticTaskAssignor[ID](
  clientsMetadata: Map[ID, ClientStateAndHostInfo[ID]],
  taskIds: Set[TaskId],
  serviceShardPartitioner: ServiceShardPartitioner,
  dynamicAssignments: Boolean = false)
    extends TaskAssignor[ID, TaskId]
    with Logging {

  //Note: We need to use LoadedStatsReceiver, since the parent class is created by Kafka without access to a scoped statsReceiver
  private val clientsMetadataSizeStat = LoadedStatsReceiver.stat("clientsMetadataSize")

  /* Public */

  //Note: Synchronized to protect against 2 assignors concurrently running when performing local feature testing....
  override def assign(numStandbyReplicas: Int): Unit = synchronized {
    assert(
      numStandbyReplicas <= 1,
      "Num standby replicas > 1 not currently supported in static task assignor"
    )
    clientsMetadataSizeStat.add(clientsMetadata.size)
    info("clientsMetadata: " + clientsMetadata.mkString(", "))

    val assignmentsMap = scala.collection.mutable.Map[HostInfo, TaskAssignments]()
    val shardIdToClientStateAndHostInfo = createInstanceToClientStateAndHostInfo()
    for {
      (groupId, tasks) <- tasksByGroupId(numStandbyReplicas, shardIdToClientStateAndHostInfo)
      taskId <- tasks
    } {
      val kafkaPartitionId = KafkaPartitionId(taskId.partition)
      val activeShardId = serviceShardPartitioner.activeShardId(kafkaPartitionId)
      val standbyShardId = getStandbyShardId(numStandbyReplicas, kafkaPartitionId)
      debug(s"TaskId $taskId StaticActive $activeShardId StaticStandby $standbyShardId")

      for (activeClientStateAndHostInfo <- lookupActiveClientState(
          shardIdToClientStateAndHostInfo,
          activeShardId,
          standbyShardId
        )) {
        val standbyClientStateAndHostInfo = getStandbyHostInfo(
          activeClientStateAndHostInfo,
          numStandbyReplicas,
          shardIdToClientStateAndHostInfo,
          standbyShardId
        )
        assign(assignmentsMap, taskId, activeClientStateAndHostInfo, standbyClientStateAndHostInfo)
      }
    }
    info(s"Assignments\n${assignmentsMap.iterator.toSeq
      .sortBy(_._1.host()).map { case (k, v) => k + "\t" + v.toPrettyStr }.mkString("\n")}")
  }

  /* Private */

  private def getStandbyHostInfo(
    activeClientStateAndHostInfo: ClientStateAndHostInfo[ID],
    numStandbyReplicas: Int,
    shardIdToClientStateAndHostInfo: Map[ServiceShardId, ClientStateAndHostInfo[ID]],
    standbyShardId: Option[ServiceShardId]
  ): Option[ClientStateAndHostInfo[ID]] = {
    val standbyHostInfo = standbyShardId.flatMap(
      lookupStandbyClientState(numStandbyReplicas, shardIdToClientStateAndHostInfo, _)
    )

    if (standbyHostInfo.contains(activeClientStateAndHostInfo)) {
      None
    } else {
      standbyHostInfo
    }
  }

  private def getStandbyShardId(numStandbyReplicas: Int, kafkaPartitionId: KafkaPartitionId) = {
    if (numStandbyReplicas == 0) {
      None
    } else {
      serviceShardPartitioner.standbyShardIds(kafkaPartitionId).headOption
    }
  }

  private def assign(
    assignmentsMap: mutable.Map[HostInfo, TaskAssignments],
    taskId: TaskId,
    activeClientStateAndHostInfo: ClientStateAndHostInfo[ID],
    standbyClientStateAndHostInfoOpt: Option[ClientStateAndHostInfo[ID]]
  ): Unit = {
    if (isPreRestoringInstance(activeClientStateAndHostInfo)) {
      standbyClientStateAndHostInfoOpt match {
        case Some(standbyClientStateAndHostInfo)
            if !isPreRestoringInstance(standbyClientStateAndHostInfo) =>
          assignStandby(assignmentsMap, taskId, activeClientStateAndHostInfo, preRestore = true)
          assignActive(assignmentsMap, taskId, standbyClientStateAndHostInfo)
        case Some(standbyClientStateAndHostInfo) =>
          assignStandby(assignmentsMap, taskId, activeClientStateAndHostInfo, preRestore = true)
        case None =>
          assignActive(assignmentsMap, taskId, activeClientStateAndHostInfo)
      }
    } else {
      assignActive(assignmentsMap, taskId, activeClientStateAndHostInfo)

      standbyClientStateAndHostInfoOpt match {
        case Some(standbyClientStateAndHostInfo)
            if !isPreRestoringInstance(standbyClientStateAndHostInfo) =>
          assignStandby(assignmentsMap, taskId, standbyClientStateAndHostInfo, preRestore = false)
        case _ =>
      }
    }
  }

  private def tasksByGroupId(
    numStandbyReplicas: Int,
    shardIdToClientStateAndHostInfo: Map[ServiceShardId, ClientStateAndHostInfo[ID]]
  ): Map[KafkaGroupId, Seq[TaskId]] = {
    val tasksByGroupId = taskIds
      .groupBy(taskId => KafkaGroupId(taskId.topicGroupId)).mapValues(_.toSeq.sortBy(_.partition))
    info(
      s"Assign with $serviceShardPartitioner and numStandbyReplicas $numStandbyReplicas \nTasksByGroupId:\n$tasksByGroupId\nInstanceToClientStateAndHostInfo:\n$shardIdToClientStateAndHostInfo"
    )
    tasksByGroupId
  }

  private def createInstanceToClientStateAndHostInfo(
  ): Map[ServiceShardId, ClientStateAndHostInfo[ID]] = {
    for ((id, clientStateAndHostInfo) <- clientsMetadata) yield {
      clientStateAndHostInfo.serviceShardId -> clientStateAndHostInfo
    }
  }

  private def assignActive(
    assignmentsMap: mutable.Map[HostInfo, TaskAssignments],
    taskId: TaskId,
    clientStateAndHostInfo: ClientStateAndHostInfo[ID]
  ) = {
    debug(s"assignActive $taskId $clientStateAndHostInfo")
    clientStateAndHostInfo.clientState.assign(taskId, true)
    val taskAssignments =
      assignmentsMap.getOrElseUpdate(clientStateAndHostInfo.hostInfo, TaskAssignments())
    taskAssignments.activeTasks += taskId
  }

  private def assignStandby(
    assignmentsMap: mutable.Map[HostInfo, TaskAssignments],
    taskId: TaskId,
    clientStateAndHostInfo: ClientStateAndHostInfo[ID],
    preRestore: Boolean
  ) = {
    debug(s"assignStandby ${if (preRestore) "PreRestore" else ""} $taskId $clientStateAndHostInfo")
    clientStateAndHostInfo.clientState.assign(taskId, false)
    val taskAssignments =
      assignmentsMap.getOrElseUpdate(clientStateAndHostInfo.hostInfo, TaskAssignments())
    if (preRestore) {
      taskAssignments.standbyPreRestoreTasks += taskId
    } else {
      taskAssignments.standbyTasks += taskId
    }
  }

  private def isPreRestoringInstance(clientStateAndHostInfo: ClientStateAndHostInfo[ID]) = {
    clientStateAndHostInfo.hostInfo.port() == StaticPartitioning.PreRestoreSignalingPort
  }

  private def lookupActiveClientState(
    instanceToClientStateAndHostInfo: Map[ServiceShardId, ClientStateAndHostInfo[ID]],
    activeInstance: ServiceShardId,
    standbyInstance: Option[ServiceShardId]
  ): Option[ClientStateAndHostInfo[ID]] = {
    instanceToClientStateAndHostInfo
      .get(activeInstance).orElse(standbyInstance.flatMap(instanceToClientStateAndHostInfo.get)).orElse(
        getNextAvailableInstance(instanceToClientStateAndHostInfo, activeInstance)
      )
  }

  //TODO: Refactor
  private def getNextAvailableInstance(
    instanceToClientStateAndHostInfo: Map[ServiceShardId, ClientStateAndHostInfo[ID]],
    activeInstance: ServiceShardId
  ): Option[ClientStateAndHostInfo[ID]] = {
    if (dynamicAssignments) {
      var nextAvailableInstance: Option[ClientStateAndHostInfo[ID]] = None
      var idx = activeInstance.id
      while (nextAvailableInstance.isEmpty) {
        idx += 1
        nextAvailableInstance = instanceToClientStateAndHostInfo.get(ServiceShardId(idx))
      }
      nextAvailableInstance
    } else {
      None
    }
  }

  private def lookupStandbyClientState(
    numStandbyReplicas: Int,
    instanceToClientStateAndHostInfo: Map[ServiceShardId, ClientStateAndHostInfo[ID]],
    shardId: ServiceShardId
  ): Option[ClientStateAndHostInfo[ID]] = {
    if (numStandbyReplicas > 0) {
      instanceToClientStateAndHostInfo.get(shardId)
    } else {
      None
    }
  }
}
