package com.twitter.finatra.kafka.producers

import com.twitter.finagle.context.Contexts
import com.twitter.finagle.filter.PayloadSizeFilter.ClientReqTraceKey
import com.twitter.finagle.tracing._
import com.twitter.finagle.{Dtab, Init}
import com.twitter.inject.Logging
import java.util.Properties
import java.util.concurrent.Future
import org.apache.kafka.clients.producer.{
  Callback,
  KafkaProducer,
  ProducerConfig,
  ProducerRecord,
  RecordMetadata
}
import org.apache.kafka.common.serialization.Serializer
import scala.collection.JavaConverters._
import com.twitter.finatra.kafka.tracingEnabled

object TracingKafkaProducer {

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

  /**
   * A Kafka Header to make the traceId of the producer visible to the consumer.
   */
  val TraceIdHeader = "producer.traceId"

  /**
   * A local context [[Contexts.local.Key key]] for setting a [[TraceId]] for testing.
   */
  private[finatra] val TestTraceIdKey: Contexts.local.Key[TraceId] =
    Contexts.local.newKey[TraceId]()

  /**
   * Helper constructor which accepts [[Properties]] for constructing the [[TracingKafkaProducer]].
   *
   * @param configs The producer configs
   * @param keySerializer The serializer for key that implements [[Serializer]]. The <pre>configure()</pre>
   *                      method won't be called in the producer when the serializer is passed
   *                      directly.
   * @param valueSerializer The serializer for value that implements [[Serializer]]. The
   *                        <pre>configure()</pre> method won't be called in the producer when the
   *                        serializer is passed in directly.
   * @tparam K type of Key
   * @tparam V type of value
   * @return an instance of [[TracingKafkaProducer]]
   */
  def apply[K, V](
    configs: Properties,
    keySerializer: Serializer[K],
    valueSerializer: Serializer[V]
  ): TracingKafkaProducer[K, V] = {
    new TracingKafkaProducer(configs.asScala.toMap, keySerializer, valueSerializer)
  }
}

/**
 * An extension of [[KafkaProducer KafkaProducer]] with Zipkin tracing to trace the records sent to
 * Kafka. This is inspired by openzipkin's <a href="https://github.com/openzipkin/brave">brave</a>
 * instrumentation. We need a custom implementation and not a
 * [[org.apache.kafka.clients.producer.ProducerInterceptor ProducerInterceptor]] because to enable
 * end-to-end tracing of the records sent to Kafka, we need the same [[TraceId trace id]] in the
 * [[Callback callback]] which is not possible in the interceptor because it's called in a
 * background thread which is not Finagle controlled.
 *
 * @param configs The producer configs
 * @param keySerializer The serializer for key that implements [[Serializer]]. The <pre>configure()</pre>
 *                      method won't be called in the producer when the serializer is passed in
 *                      directly.
 * @param valueSerializer The serializer for value that implements [[Serializer]]. The
 *                        <pre>configure()</pre> method won't be called in the producer when the
 *                        serializer is passed in directly.
 * @tparam K type of Key
 * @tparam V type of value
 */
class TracingKafkaProducer[K, V](
  configs: Map[String, AnyRef],
  keySerializer: Serializer[K],
  valueSerializer: Serializer[V])
    extends KafkaProducer[K, V](configs.asJava, keySerializer, valueSerializer)
    with Logging {

  import TracingKafkaProducer._

  override def send(record: ProducerRecord[K, V], callback: Callback): Future[RecordMetadata] = {
    val shouldTrace = tracingEnabled()
    withTracing { trace =>
      if (trace.isActivelyTracing && shouldTrace) {
        info(s"Tracing producer record with trace id: ${trace.id}")
        addSendTraceAnnotations(trace, record)
        try {
          record.headers().add(TraceIdHeader, TraceId.serialize(trace.id))
        } catch {
          case ex: IllegalStateException =>
            warn(
              s"[traceId=${trace.id.traceId.toString}] Unable to add TraceId header to the producer record",
              ex)
        }
      }
      super.send(
        record,
        new Callback { // kept for sbt compatibility
          override def onCompletion(metadata: RecordMetadata, exception: Exception): Unit = {
            if (trace.isActivelyTracing && shouldTrace) {
              addReceiveTraceAnnotations(trace, metadata, exception)
            }
            if (callback != null) {
              callback.onCompletion(metadata, exception)
            }
          }
        }
      )
    }
  }

  private def withTracing[R](f: Tracing => R): R = {
    // NOTE: This code can be called from a non-finagle controlled thread and hence the
    // Trace.tracers can be empty as it's absent from Contexts.local.
    val tracer = if (Trace.tracers.isEmpty) DefaultTracer.self else BroadcastTracer(Trace.tracers)
    val nextTraceId = Contexts.local.get(TestTraceIdKey).getOrElse(Trace.nextId)
    Trace.letTracerAndId(tracer, nextTraceId) {
      val trace = Trace()
      f(trace)
    }
  }

  private def addSendTraceAnnotations(trace: Tracing, record: ProducerRecord[K, V]): Unit = {
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
    configs.get(KafkaProducerConfig.FinagleDestKey) match {
      case Some(dest) => trace.recordBinary("clnt/namer.path", dest)
      case None => // nop
    }
    configs.get(ProducerConfig.CLIENT_ID_CONFIG) match {
      case Some(clientId) => trace.recordBinary(ClientIdAnnotation, clientId)
      case None => // nop
    }
    trace.recordBinary(ProducerTopicAnnotation, record.topic())
    trace.recordRpc("send")
    trace.record(Annotation.ClientSend)
    trace.record(Annotation.WireSend)
    trace.record(ProducerSendAnnotation)
  }

  private def addReceiveTraceAnnotations(
    trace: Tracing,
    metadata: RecordMetadata,
    exception: Exception
  ): Unit = {
    if (metadata != null) {
      val totalSize = metadata.serializedKeySize() + metadata.serializedValueSize()
      trace.recordBinary(ClientReqTraceKey, totalSize)
    } else {
      trace.recordBinary(ClientReqTraceKey, 0)
    }
    val someExceptionMessage = Option(exception).map(e => s"${e.getClass.getName} ${e.getMessage}")
    // Order is important here
    someExceptionMessage.foreach(msg => trace.record(Annotation.WireRecvError(msg)))
    trace.record(Annotation.WireRecv)
    someExceptionMessage.foreach(msg => trace.record(Annotation.ClientRecvError(msg)))
    trace.record(Annotation.ClientRecv)
  }
}
