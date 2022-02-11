package com.twitter.finatra.kafka.consumers

import com.twitter.finagle.filter.PayloadSizeFilter.ClientRepTraceKey
import com.twitter.finagle.tracing.Annotation
import com.twitter.finagle.tracing.TraceServiceName
import com.twitter.finagle.tracing.Tracing
import com.twitter.finagle.Dtab
import com.twitter.finagle.Init
import com.twitter.util.logging.Logging
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.consumer.ConsumerRecords
import scala.collection.JavaConverters._

object KafkaConsumerTraceAnnotatorImpl {

  /**
   * Trace annotation for the topic(s) subscribed by the consumer subscribes. This is present as a
   * [[com.twitter.finagle.tracing.Annotation.BinaryAnnotation BinaryAnnotation]] in a trace.
   */
  val ConsumerTopicAnnotation = "clnt/kafka.consumer.topic"

  /**
   * Trace annotation to record the poll event.
   */
  val ConsumerPollAnnotation = "clnt/kafka.consumer.poll"

  /**
   * Trace annotation for the group id defined inside [[org.apache.kafka.clients.consumer.ConsumerConfig.GROUP_ID_CONFIG GROUP_ID_CONFIG]].
   * This is present as a [[com.twitter.finagle.tracing.Annotation.BinaryAnnotation BinaryAnnotation]]
   * in the trace.
   */
  val ConsumerGroupIdAnnotation = "clnt/kafka.consumer.groupId"

  /**
   * Trace annotation for the client id defined inside [[org.apache.kafka.clients.consumer.ConsumerConfig.CLIENT_ID_CONFIG CLIENT_ID_CONFIG]].
   * This is present as a [[com.twitter.finagle.tracing.Annotation.BinaryAnnotation BinaryAnnotation]]
   * in the trace.
   */
  val ClientIdAnnotation = "kafka.clientId"
}

/**
 * An implementation of [[KafkaConsumerTraceAnnotator]] which records standard annotations for a
 * Kafka consumer's `poll` operation.
 */
class KafkaConsumerTraceAnnotatorImpl extends KafkaConsumerTraceAnnotator with Logging {
  import KafkaConsumerTraceAnnotatorImpl._

  override def recordAnnotations[K, V](
    trace: Tracing,
    records: ConsumerRecords[K, V],
    consumerConfig: Map[String, AnyRef]
  ): Unit = {
    val scalaRecords: Iterable[ConsumerRecord[K, V]] = records.asScala
    val serviceName = TraceServiceName() match {
      case Some(name) => name
      case None => "kafka.consumer"
    }
    trace.recordServiceName(serviceName)
    trace.recordBinary("clnt/finagle.label", serviceName)
    trace.recordBinary("clnt/finagle.version", Init.finagleVersion)
    if (Dtab.local.nonEmpty) {
      trace.recordBinary("clnt/dtab.local", Dtab.local.show)
    }
    if (Dtab.limited.nonEmpty) {
      trace.recordBinary("clnt/dtab.limited", Dtab.limited.show)
    }
    consumerConfig.get(KafkaConsumerConfig.FinagleDestKey) match {
      case Some(dest) => trace.recordBinary("clnt/namer.path", dest)
      case None => // nop
    }
    consumerConfig.get(ConsumerConfig.CLIENT_ID_CONFIG) match {
      case Some(clientId) => trace.recordBinary(ClientIdAnnotation, clientId)
      case None => // nop
    }
    consumerConfig.get(ConsumerConfig.GROUP_ID_CONFIG) match {
      case Some(groupId) => trace.recordBinary(ConsumerGroupIdAnnotation, groupId)
      case None => // nop
    }
    extractTopics(scalaRecords).foreach(topic => trace.recordBinary(ConsumerTopicAnnotation, topic))
    trace.recordBinary(ClientRepTraceKey, computeTotalResponseSize(scalaRecords))
    trace.recordRpc("poll")
    trace.record(ConsumerPollAnnotation)
    trace.record(Annotation.WireRecv)
    trace.record(Annotation.ClientRecv)
  }

  private def extractTopics[K, V](records: Iterable[ConsumerRecord[K, V]]): Set[String] = {
    records.map(_.topic()).toSet
  }

  private def computeTotalResponseSize[K, V](records: Iterable[ConsumerRecord[K, V]]): Int = {
    records.foldLeft(0) {
      case (sum, record) => sum + record.serializedKeySize() + record.serializedValueSize()
    }
  }
}
