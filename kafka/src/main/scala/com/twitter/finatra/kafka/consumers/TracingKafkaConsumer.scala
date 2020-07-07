package com.twitter.finatra.kafka.consumers

import com.twitter.finagle.filter.PayloadSizeFilter.ClientRepTraceKey
import com.twitter.finagle.server.ServerInfo
import com.twitter.finagle.tracing._
import com.twitter.finagle.{Dtab, Init}
import com.twitter.finatra.kafka
import com.twitter.finatra.kafka.producers.TracingKafkaProducer.TraceIdHeader
import com.twitter.inject.Logging
import com.twitter.util.{Return, Throw}
import java.time.Duration
import java.util.Properties
import org.apache.kafka.clients.consumer.{
  ConsumerConfig,
  ConsumerRecord,
  ConsumerRecords,
  KafkaConsumer
}
import org.apache.kafka.common.header.Header
import org.apache.kafka.common.serialization.Deserializer
import scala.collection.JavaConverters._

object TracingKafkaConsumer {

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
   * Trace annotation for the group id defined inside [[ConsumerConfig.GROUP_ID_CONFIG GROUP_ID_CONFIG]].
   * This is present as a [[com.twitter.finagle.tracing.Annotation.BinaryAnnotation BinaryAnnotation]]
   * in the trace.
   */
  val ConsumerGroupIdAnnotation = "clnt/kafka.consumer.groupId"

  /**
   * Trace annotation for the client id defined inside [[ConsumerConfig.CLIENT_ID_CONFIG CLIENT_ID_CONFIG]].
   * This is present as a [[com.twitter.finagle.tracing.Annotation.BinaryAnnotation BinaryAnnotation]]
   * in the trace.
   */
  val ClientIdAnnotation = "kafka.clientId"

  /**
   * Trace annotation for the traceId of the publisher that published the record. This is present as
   * a [[com.twitter.finagle.tracing.Annotation.BinaryAnnotation BinaryAnnotation]] in a trace.
   */
  val ProducerTraceIdAnnotation = "clnt/kafka.producer.traceId"

  /**
   * Helper constructor which accepts [[Properties]] for constructing the [[TracingKafkaConsumer]].
   *
   * @param configs The consumer configs.
   * @param keyDeserializer The serializer for key that implements [[Deserializer]]. The <pre>configure()</pre>
   *                        method won't be called in the consumer when the deserializer is passed in
   *                        directly.
   * @param valueDeserializer The serializer for value that implements [[Deserializer]]. The
   *                          <pre>configure()</pre> method won't be called in the consumer when the
   *                          deserializer is passed in directly.
   * @tparam K type of Key.
   * @tparam V type of value.
   * @return an instance of [[TracingKafkaConsumer]].
   */
  def apply[K, V](
    configs: Properties,
    keyDeserializer: Deserializer[K],
    valueDeserializer: Deserializer[V]
  ): TracingKafkaConsumer[K, V] =
    new TracingKafkaConsumer(configs.asScala.toMap, keyDeserializer, valueDeserializer)

  private val TracingEnabledToggle = kafka.Toggles(kafka.TracingEnabledToggleId)

  private val TraceIdSampled = Some(true)

  private val TraceIdIsDebug = (traceId: TraceId) => traceId.flags.isDebug
}

/**
 * An extension of [[KafkaConsumer KafkaConsumer]] with Zipkin tracing to trace the records received
 * from Kafka. This is inspired by openzipkin's <a href="https://github.com/openzipkin/brave">brave</a>
 * instrumentation.
 *
 * @param configs The consumer configs.
 * @param keyDeserializer The serializer for key that implements [[Deserializer]]. The <pre>configure()</pre>
 *                        method won't be called in the consumer when the deserializer is passed in
 *                        directly.
 * @param valueDeserializer The serializer for value that implements [[Deserializer]]. The
 *                          <pre>configure()</pre> method won't be called in the consumer when the
 *                          deserializer is passed in directly.
 * @tparam K type of Key.
 * @tparam V type of value.
 */
class TracingKafkaConsumer[K, V](
  configs: Map[String, AnyRef],
  keyDeserializer: Deserializer[K],
  valueDeserializer: Deserializer[V])
    extends KafkaConsumer[K, V](configs.asJava, keyDeserializer, valueDeserializer)
    with Logging {

  import TracingKafkaConsumer._

  override def poll(timeout: Duration): ConsumerRecords[K, V] = {
    val records = super.poll(timeout)
    if (!records.isEmpty && TracingEnabledToggle(ServerInfo().id.hashCode)) {
      val scalaRecords: Iterable[ConsumerRecord[K, V]] = records.asScala
      // Will only trace if the producer trace id is sampled or has debug flag set to true
      val sampledTraceIds = extractSampledTraceIds(scalaRecords)
      val isSampled = sampledTraceIds.nonEmpty
      val isDebug = sampledTraceIds.exists(TraceIdIsDebug)
      // NOTE: Currently this is mostly going to create single span traces and the trace id might
      // not propagate back to the caller.
      withTracing(isSampled, isDebug) { trace =>
        if (trace.isActivelyTracing) {
          info(s"Tracing consumer records with trace id: ${trace.id}")
          addTraceAnnotations(trace, scalaRecords, sampledTraceIds)
        }
      }
    }
    records
  }

  private def withTracing(sampled: Boolean, isDebug: Boolean)(f: Tracing => Unit): Unit = {
    // NOTE: This code can be called from a non-finagle controlled thread and hence the
    // Trace.tracers can be empty as it's absent from Contexts.local
    val tracer = if (Trace.tracers.isEmpty) DefaultTracer.self else BroadcastTracer(Trace.tracers)
    val nextTraceId: TraceId = {
      val nextId = Trace.nextId.copy(_sampled = Some(sampled))
      if (isDebug) nextId.copy(flags = nextId.flags.setDebug)
      else nextId
    }
    Trace.letTracerAndId(tracer, nextTraceId) {
      val trace = Trace()
      f(trace)
    }
  }

  private def extractSampledTraceIds(records: Iterable[ConsumerRecord[K, V]]): Set[TraceId] = {
    val traceIdHeaders = records.flatMap(extractTraceIdHeaders)
    val traceIds = traceIdHeaders.flatMap(deserializeTraceId)
    traceIds.toSet
  }

  private def deserializeTraceId(header: Header): Option[TraceId] = {
    TraceId.deserialize(header.value()) match {
      case Return(traceId) if (traceId.sampled == TraceIdSampled) || TraceIdIsDebug(traceId) =>
        Some(traceId)

      case Return(_) => None

      case Throw(ex) =>
        warn(s"Unable to deserialize trace id from header: $header", ex)
        None
    }
  }

  private def extractTraceIdHeaders(record: ConsumerRecord[K, V]): Iterable[Header] = {
    record.headers().headers(TraceIdHeader).asScala
  }

  private def extractTopics(records: Iterable[ConsumerRecord[K, V]]): Set[String] = {
    records.map(_.topic()).toSet
  }

  private def computeTotalResponseSize(records: Iterable[ConsumerRecord[K, V]]): Int = {
    records.foldLeft(0) {
      case (sum, record) => sum + record.serializedKeySize() + record.serializedValueSize()
    }
  }

  private def addTraceAnnotations(
    trace: Tracing,
    records: Iterable[ConsumerRecord[K, V]],
    producerTraceIds: Set[TraceId]
  ): Unit = {
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
    configs.get(KafkaConsumerConfig.FinagleDestKey) match {
      case Some(dest) => trace.recordBinary("clnt/namer.path", dest)
      case None => // nop
    }
    configs.get(ConsumerConfig.CLIENT_ID_CONFIG) match {
      case Some(clientId) => trace.recordBinary(ClientIdAnnotation, clientId)
      case None => // nop
    }
    configs.get(ConsumerConfig.GROUP_ID_CONFIG) match {
      case Some(groupId) => trace.recordBinary(ConsumerGroupIdAnnotation, groupId)
      case None => // nop
    }
    extractTopics(records).foreach(topic => trace.recordBinary(ConsumerTopicAnnotation, topic))
    trace.recordBinary(ClientRepTraceKey, computeTotalResponseSize(records))
    debug(s"[traceId=${trace.id.traceId.toString}] Recording producer trace ids: $producerTraceIds")
    producerTraceIds.foreach(id => trace.recordBinary(ProducerTraceIdAnnotation, id.toString))
    trace.recordRpc("poll")
    trace.record(ConsumerPollAnnotation)
    trace.record(Annotation.WireRecv)
    trace.record(Annotation.ClientRecv)
  }

}
