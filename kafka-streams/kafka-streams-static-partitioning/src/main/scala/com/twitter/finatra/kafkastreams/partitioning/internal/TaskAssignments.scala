package com.twitter.finatra.kafkastreams.partitioning.internal

import org.apache.kafka.streams.processor.TaskId
import scala.collection.mutable.ArrayBuffer

case class TaskAssignments(
  activeTasks: ArrayBuffer[TaskId] = ArrayBuffer(),
  standbyTasks: ArrayBuffer[TaskId] = ArrayBuffer(),
  standbyPreRestoreTasks: ArrayBuffer[TaskId] = ArrayBuffer()) {

  def toPrettyStr: String = {
    s"Active:\t${activeTasks.mkString(", ")}" +
      s"\tStandby:\t${standbyTasks.mkString(", ")}" +
      s"\tPreRestore:\t${standbyPreRestoreTasks.mkString(", ")}"
  }
}
