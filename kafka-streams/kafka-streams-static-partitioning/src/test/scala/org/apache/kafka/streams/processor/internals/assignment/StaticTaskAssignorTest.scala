package org.apache.kafka.streams.processor.internals.assignment

import com.twitter.finatra.kafkastreams.partitioning.StaticPartitioning
import com.twitter.finatra.kafkastreams.partitioning.internal.{
  ClientStateAndHostInfo,
  StaticTaskAssignor
}
import com.twitter.finatra.streams.queryable.thrift.partitioning.StaticServiceShardPartitioner
import com.twitter.inject.Test
import java.util
import org.apache.kafka.streams.processor.TaskId
import org.apache.kafka.streams.state.HostInfo
import scala.collection.JavaConverters._

class StaticTaskAssignorTest extends Test {

  private val task00 = new TaskId(0, 0)
  private val task01 = new TaskId(0, 1)
  private val task02 = new TaskId(0, 2)
  private val task03 = new TaskId(0, 3)
  private val task04 = new TaskId(0, 4)
  private val task05 = new TaskId(0, 5)
  private val task06 = new TaskId(0, 6)
  private val task07 = new TaskId(0, 7)
  private val task08 = new TaskId(0, 8)

  private val clients = new util.HashMap[Int, ClientStateAndHostInfo[Int]]()

  override def beforeEach(): Unit = {
    super.beforeEach()
    clients.clear()
  }

  test("testAssign") {
    createClient(processId = 0, capacity = 1)
    createClient(processId = 1, capacity = 1)
    createClient(processId = 2, capacity = 1)
    createClient(processId = 3, capacity = 1)

    val assignor = new StaticTaskAssignor[Int](
      serviceShardPartitioner = StaticServiceShardPartitioner(numShards = 4),
      clientsMetadata = clients.asScala.toMap,
      taskIds = Set(task00, task01, task02, task03)
    )

    val numStandbyReplicas = 1
    assignor.assign(numStandbyReplicas)

    assertTasks(instanceId = 0, active = Set(task00), standby = Set(task02))

    assertTasks(instanceId = 1, active = Set(task01), standby = Set(task03))

    assertTasks(instanceId = 2, active = Set(task02), standby = Set(task00))

    assertTasks(instanceId = 3, active = Set(task03), standby = Set(task01))
  }

  test("testAssign Side A down") {
    createClient(processId = 2, capacity = 1)
    createClient(processId = 3, capacity = 1)

    val assignor = new StaticTaskAssignor[Int](
      serviceShardPartitioner = StaticServiceShardPartitioner(numShards = 4),
      clientsMetadata = clients.asScala.toMap,
      taskIds = Set(task00, task01, task02, task03)
    )

    val numStandbyReplicas = 1
    assignor.assign(numStandbyReplicas)

    assertTasks(instanceId = 2, active = Set(task00, task02), standby = Set())

    assertTasks(instanceId = 3, active = Set(task01, task03), standby = Set())
  }

  test("testAssign Side B down") {
    createClient(processId = 0, capacity = 1)
    createClient(processId = 1, capacity = 1)

    val assignor = new StaticTaskAssignor[Int](
      serviceShardPartitioner = StaticServiceShardPartitioner(numShards = 4),
      clientsMetadata = clients.asScala.toMap,
      taskIds = Set(task00, task01, task02, task03)
    )

    val numStandbyReplicas = 1
    assignor.assign(numStandbyReplicas)

    assertTasks(instanceId = 0, active = Set(task00, task02), standby = Set())

    assertTasks(instanceId = 1, active = Set(task01, task03), standby = Set())
  }

  test("testAssign with 0 standby replicas") {
    createClient(processId = 0, capacity = 1)
    createClient(processId = 1, capacity = 1)
    createClient(processId = 2, capacity = 1)
    createClient(processId = 3, capacity = 1)

    val assignor = new StaticTaskAssignor[Int](
      serviceShardPartitioner = StaticServiceShardPartitioner(numShards = 4),
      clientsMetadata = clients.asScala.toMap,
      taskIds = Set(task00, task01, task02, task03)
    )

    val numStandbyReplicas = 0
    assignor.assign(numStandbyReplicas)

    assertTasks(instanceId = 0, active = Set(task00), standby = Set())

    assertTasks(instanceId = 1, active = Set(task01), standby = Set())

    assertTasks(instanceId = 2, active = Set(task02), standby = Set())

    assertTasks(instanceId = 3, active = Set(task03), standby = Set())
  }

  test("assign with task 01 active instance down") {
    createClient(0, 1)
    createClient(2, 1)
    createClient(3, 1)

    val assignor = new StaticTaskAssignor[Int](
      serviceShardPartitioner = StaticServiceShardPartitioner(numShards = 4),
      clientsMetadata = clients.asScala.toMap,
      taskIds = Set(task00, task01, task02, task03)
    )

    val numStandbyReplicas = 1
    assignor.assign(numStandbyReplicas)

    assertTasks(instanceId = 0, active = Set(task00), standby = Set(task02))

    assertTasks(instanceId = 2, active = Set(task02), standby = Set(task00))

    assertTasks(instanceId = 3, active = Set(task03, task01), standby = Set())
  }

  test(
    "don't dynamically assign task00 when active and standby instances down and dynamicAssignments = false"
  ) {
    createClient(processId = 1, capacity = 1)
    createClient(processId = 3, capacity = 1)

    val assignor = new StaticTaskAssignor[Int](
      serviceShardPartitioner = StaticServiceShardPartitioner(numShards = 4),
      clientsMetadata = clients.asScala.toMap,
      taskIds = Set(task00, task01, task02, task03),
      dynamicAssignments = false
    )

    val numStandbyReplicas = 1
    assignor.assign(numStandbyReplicas)

    assertTasks(instanceId = 1, active = Set(task01), standby = Set(task03))

    assertTasks(instanceId = 3, active = Set(task03), standby = Set(task01))
  }

  test("dont assign active and standby to single instance") {
    createClient(0, 1)

    val assignor = new StaticTaskAssignor[Int](
      serviceShardPartitioner = StaticServiceShardPartitioner(numShards = 1),
      clientsMetadata = clients.asScala.toMap,
      taskIds = Set(task00)
    )

    val numStandbyReplicas = 1
    assignor.assign(numStandbyReplicas)

    assertTasks(instanceId = 0, active = Set(task00), standby = Set())
  }

  test("assign standbys to 2 of 2 clients") {
    createClient(0, 2)
    createClient(1, 2)

    val assignor = new StaticTaskAssignor[Int](
      serviceShardPartitioner = StaticServiceShardPartitioner(numShards = 2),
      clientsMetadata = clients.asScala.toMap,
      taskIds = Set(task00, task01)
    )

    val numStandbyReplicas = 1
    assignor.assign(numStandbyReplicas)

    assertTasks(instanceId = 0, active = Set(task00), standby = Set(task01))

    assertTasks(instanceId = 1, active = Set(task01), standby = Set(task00))
  }

  test("assign standbys to 3 of 3 clients") {
    createClient(0, 1)
    createClient(1, 1)
    createClient(2, 1)

    val assignor = new StaticTaskAssignor[Int](
      serviceShardPartitioner = StaticServiceShardPartitioner(numShards = 3),
      clientsMetadata = clients.asScala.toMap,
      taskIds = Set(task00, task01, task02)
    )

    val numStandbyReplicas = 1
    assignor.assign(numStandbyReplicas)

    assertTasks(instanceId = 0, active = Set(task00), standby = Set(task02))

    assertTasks(instanceId = 1, active = Set(task01), standby = Set(task00))

    assertTasks(instanceId = 2, active = Set(task02), standby = Set(task01))
  }

  test("assign standbys to 2 of 3 clients") {
    createClient(0, 2)
    createClient(1, 2)

    val assignor = new StaticTaskAssignor[Int](
      serviceShardPartitioner = StaticServiceShardPartitioner(numShards = 3),
      clientsMetadata = clients.asScala.toMap,
      taskIds = Set(task00, task01, task02)
    )

    val numStandbyReplicas = 1
    assignor.assign(numStandbyReplicas)

    assertTasks(instanceId = 0, active = Set(task00, task02), standby = Set())

    assertTasks(instanceId = 1, active = Set(task01), standby = Set(task00))
  }

  test("instance 0 should get 1 active and 1 standby of the 100 other tasks") {
    createClient(0, 2)
    createClient(50, 2)

    val assignor = new StaticTaskAssignor[Int](
      serviceShardPartitioner = StaticServiceShardPartitioner(numShards = 100),
      clientsMetadata = clients.asScala.toMap,
      taskIds = createTasks(100),
      dynamicAssignments = false
    )

    val numStandbyReplicas = 1
    assignor.assign(numStandbyReplicas)

    assertTasks(instanceId = 0, active = Set(task00), standby = Set(new TaskId(0, 50)))
  }

  test(
    "instance 0 should get 2 active when standby replicas disabled with 50 aurora shards and 100 partitions"
  ) {
    createClient(0, 1)

    val assignor = new StaticTaskAssignor[Int](
      serviceShardPartitioner = StaticServiceShardPartitioner(numShards = 50),
      clientsMetadata = clients.asScala.toMap,
      taskIds = createTasks(100),
      dynamicAssignments = false
    )

    val numStandbyReplicas = 0
    assignor.assign(numStandbyReplicas)

    assertTasks(instanceId = 0, active = Set(new TaskId(0, 0), new TaskId(0, 50)), standby = Set())
  }

  def createTasks(numPartitions: Int) = {
    (for (partition <- 0 until numPartitions) yield {
      new TaskId(0, partition)
    }).toSet
  }

  //TODO: Spread out standby tasks so they do not all move to the same client when an instance restarts
  test("9 tasks across 3 clients") {
    createClient(0, 1)
    createClient(1, 1)
    createClient(2, 1)

    val assignor = new StaticTaskAssignor[Int](
      serviceShardPartitioner = StaticServiceShardPartitioner(numShards = 3),
      clientsMetadata = clients.asScala.toMap,
      taskIds = Set(task00, task01, task02, task03, task04, task05, task06, task07, task08)
    )

    val numStandbyReplicas = 1
    assignor.assign(numStandbyReplicas)

    assertTasks(
      instanceId = 0,
      active = Set(task00, task03, task06),
      standby = Set(task02, task05, task08)
    )

    assertTasks(
      instanceId = 1,
      active = Set(task01, task04, task07),
      standby = Set(task00, task03, task06)
    )

    assertTasks(
      instanceId = 2,
      active = Set(task02, task05, task08),
      standby = Set(task01, task04, task07)
    )
  }

  test("1 task and 1 client and no standby replicas") {
    createClient(0, 1)

    val assignor = new StaticTaskAssignor[Int](
      serviceShardPartitioner = StaticServiceShardPartitioner(numShards = 3),
      clientsMetadata = clients.asScala.toMap,
      taskIds = Set(task00)
    )

    val numStandbyReplicas = 0
    assignor.assign(numStandbyReplicas)

    assertTasks(instanceId = 0, active = Set(task00), standby = Set())
  }

  test("1 task and 1 client and 1 standby replicas") {
    createClient(0, 1)

    val assignor = new StaticTaskAssignor[Int](
      serviceShardPartitioner = StaticServiceShardPartitioner(numShards = 3),
      clientsMetadata = clients.asScala.toMap,
      taskIds = Set(task00)
    )

    val numStandbyReplicas = 1
    assignor.assign(numStandbyReplicas)

    assertTasks(instanceId = 0, active = Set(task00), standby = Set())
  }

  test("3 clients, 3 tasks, with standby 0") {
    createClient(0, 1)
    createClient(1, 1)
    createClient(2, 1)

    val assignor = new StaticTaskAssignor[Int](
      serviceShardPartitioner = StaticServiceShardPartitioner(numShards = 3),
      clientsMetadata = clients.asScala.toMap,
      taskIds = Set(task00, task01, task02)
    )

    val numStandbyReplicas = 0
    assignor.assign(numStandbyReplicas)

    assertTasks(instanceId = 0, active = Set(task00), standby = Set())
    assertTasks(instanceId = 1, active = Set(task01), standby = Set())
    assertTasks(instanceId = 2, active = Set(task02), standby = Set())
  }

  test("3 clients, 3 tasks, with standby 0. Prerestore 0") {
    createClient(0, 1, preRestore = true)
    createClient(1, 1)
    createClient(2, 1)

    val assignor = new StaticTaskAssignor[Int](
      serviceShardPartitioner = StaticServiceShardPartitioner(numShards = 3),
      clientsMetadata = clients.asScala.toMap,
      taskIds = Set(task00, task01, task02)
    )

    val numStandbyReplicas = 0
    assignor.assign(numStandbyReplicas)

    assertTasks(instanceId = 0, active = Set(task00), standby = Set())
    assertTasks(instanceId = 1, active = Set(task01), standby = Set())
    assertTasks(instanceId = 2, active = Set(task02), standby = Set())
  }

  test("3 clients, 3 tasks, with standby 0. Prerestore 0, 1") {
    createClient(0, 1, preRestore = true)
    createClient(1, 1, preRestore = true)
    createClient(2, 1)

    val assignor = new StaticTaskAssignor[Int](
      serviceShardPartitioner = StaticServiceShardPartitioner(numShards = 3),
      clientsMetadata = clients.asScala.toMap,
      taskIds = Set(task00, task01, task02)
    )

    val numStandbyReplicas = 0
    assignor.assign(numStandbyReplicas)

    assertTasks(instanceId = 0, active = Set(task00), standby = Set())
    assertTasks(instanceId = 1, active = Set(task01), standby = Set())
    assertTasks(instanceId = 2, active = Set(task02), standby = Set())
  }

  test("3 clients, 3 tasks, with standby 0. Prerestore 0, 1, 2") {
    createClient(0, 1, preRestore = true)
    createClient(1, 1, preRestore = true)
    createClient(2, 1, preRestore = true)

    val assignor = new StaticTaskAssignor[Int](
      serviceShardPartitioner = StaticServiceShardPartitioner(numShards = 3),
      clientsMetadata = clients.asScala.toMap,
      taskIds = Set(task00, task01, task02)
    )

    val numStandbyReplicas = 0
    assignor.assign(numStandbyReplicas)

    assertTasks(instanceId = 0, active = Set(task00), standby = Set())
    assertTasks(instanceId = 1, active = Set(task01), standby = Set())
    assertTasks(instanceId = 2, active = Set(task02), standby = Set())
  }

  test("3 clients, 3 tasks, with standby 1") {
    createClient(0, 1)
    createClient(1, 1)
    createClient(2, 1)

    val assignor = new StaticTaskAssignor[Int](
      serviceShardPartitioner = StaticServiceShardPartitioner(numShards = 3),
      clientsMetadata = clients.asScala.toMap,
      taskIds = Set(task00, task01, task02)
    )

    val numStandbyReplicas = 1
    assignor.assign(numStandbyReplicas)

    assertTasks(instanceId = 0, active = Set(task00), standby = Set(task02))
    assertTasks(instanceId = 1, active = Set(task01), standby = Set(task00))
    assertTasks(instanceId = 2, active = Set(task02), standby = Set(task01))
  }

  test("3 clients, 3 tasks, with standby 1. Prerestore 0") {
    createClient(0, 1, preRestore = true)
    createClient(1, 1)
    createClient(2, 1)

    val assignor = new StaticTaskAssignor[Int](
      serviceShardPartitioner = StaticServiceShardPartitioner(numShards = 3),
      clientsMetadata = clients.asScala.toMap,
      taskIds = Set(task00, task01, task02)
    )

    val numStandbyReplicas = 1
    assignor.assign(numStandbyReplicas)

    assertTasks(instanceId = 0, active = Set(), standby = Set(task00))
    assertTasks(instanceId = 1, active = Set(task01, task00), standby = Set())
    assertTasks(instanceId = 2, active = Set(task02), standby = Set(task01))
  }

  test("3 clients, 3 tasks, with standby 1. Prerestore 0 and 1") {
    createClient(0, 1, preRestore = true)
    createClient(1, 1, preRestore = true)
    createClient(2, 1)

    val assignor = new StaticTaskAssignor[Int](
      serviceShardPartitioner = StaticServiceShardPartitioner(numShards = 3),
      clientsMetadata = clients.asScala.toMap,
      taskIds = Set(task00, task01, task02)
    )

    val numStandbyReplicas = 1
    assignor.assign(numStandbyReplicas)

    assertTasks(instanceId = 0, active = Set(), standby = Set(task00))
    assertTasks(instanceId = 1, active = Set(), standby = Set(task01))
    assertTasks(instanceId = 2, active = Set(task02, task01), standby = Set())
  }

  test("3 clients, 3 tasks, with standby 1. Prerestore 0 and 1 and 2") {
    createClient(0, 1, preRestore = true)
    createClient(1, 1, preRestore = true)
    createClient(2, 1, preRestore = true)

    val assignor = new StaticTaskAssignor[Int](
      serviceShardPartitioner = StaticServiceShardPartitioner(numShards = 3),
      clientsMetadata = clients.asScala.toMap,
      taskIds = Set(task00, task01, task02)
    )

    val numStandbyReplicas = 1
    assignor.assign(numStandbyReplicas)

    assertTasks(instanceId = 0, active = Set(), standby = Set(task00))
    assertTasks(instanceId = 1, active = Set(), standby = Set(task01))
    assertTasks(instanceId = 2, active = Set(), standby = Set(task02))
  }

  private def assertTasks(instanceId: Int, active: Set[TaskId], standby: Set[TaskId]): Unit = {
    val state = clients.get(instanceId)
    assert(state.clientState.activeTasks().asScala == active)
    assert(state.clientState.standbyTasks().asScala == standby)
  }

  private def createClient(processId: Int, capacity: Int, preRestore: Boolean = false) = {
    val clientState = new ClientState(capacity)
    val hostInfo = new HostInfo(
      s"$processId.foo.com",
      if (preRestore) StaticPartitioning.PreRestoreSignalingPort else 12345
    )
    clients.put(processId, ClientStateAndHostInfo(processId, clientState, hostInfo))
    clientState
  }
}
