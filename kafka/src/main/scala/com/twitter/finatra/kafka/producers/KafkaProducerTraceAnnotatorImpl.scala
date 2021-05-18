package com.twitter.finatra.kafka.producers

import com.twitter.finagle.tracing.{Annotation, TraceServiceName, Tracing}
import com.twitter.finagle.{Dtab, Init}
import com.twitter.inject.Logging
import org.apache.kafka.clients.producer.{ProducerConfig, ProducerRecord}

object KafkaProducerTraceAnnotatorImpl {

  /**
   * Trace annotation for the topic being used by the publisher. This is present as a
   * [[com.twitter.finagle.tracing.Annotation.BinaryAnnotation BinaryAnnotation]] in the trace.
   */
  val ProducerTopicAnnotation = "clnt/kafka.producer.topic"

  /**
   * Trace annotation to record the send event.
   */
  val ProducerSendAnnotation = "clnt/kafka.producer.send"

  /**
   * Trace annotation for the client id defined inside [[ProducerConfig.CLIENT_ID_CONFIG CLIENT_ID_CONFIG]].
   * This is present as a [[com.twitter.finagle.tracing.Annotation.BinaryAnnotation BinaryAnnotation]]
   * in the trace.
   */
  val ClientIdAnnotation = "kafka.clientId"
}

/**
 * An implementation of [[KafkaProducerTraceAnnotator]] which records standard annotations for a
 * Kafka producer's `send` operation.
 */
class KafkaProducerTraceAnnotatorImpl extends KafkaProducerTraceAnnotator with Logging {
  import KafkaProducerTraceAnnotatorImpl._

  override def recordAnnotations[K, V](
    trace: Tracing,
    record: ProducerRecord[K, V],
    producerConfig: Map[String, AnyRef]
  ): Unit = {
    val serviceName = TraceServiceName() match {
      case Some(name) => name
      case None => "kafka.producer"
    }
    trace.recordServiceName(serviceName)
    trace.recordBinary("clnt/finagle.label", serviceName)
    trace.recordBinary("clnt/finagle.version", Init.finagleVersion)
    if (Dtab.local.nonEmpty) {
      trace.recordBinary("clnt/dtab.local", Dtab.local.show)
    }
    producerConfig.get(KafkaProducerConfig.FinagleDestKey) match {
      case Some(dest) => trace.recordBinary("clnt/namer.path", dest)
      case None => // nop
    }
    producerConfig.get(ProducerConfig.CLIENT_ID_CONFIG) match {
      case Some(clientId) => trace.recordBinary(ClientIdAnnotation, clientId)
      case None => // nop
    }
    trace.recordBinary(ProducerTopicAnnotation, record.topic())
    trace.recordRpc("send")
    trace.record(Annotation.ClientSend)
    trace.record(Annotation.WireSend)
    trace.record(ProducerSendAnnotation)
  }
}
