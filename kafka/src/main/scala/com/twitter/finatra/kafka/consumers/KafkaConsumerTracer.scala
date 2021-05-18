package com.twitter.finatra.kafka.consumers

import com.twitter.finagle.tracing._
import com.twitter.finagle.util.LoadService
import com.twitter.finatra.kafka.producers.TracingKafkaProducer.TraceIdHeader
import com.twitter.util.logging.Logger
import com.twitter.util.{Future, Return, Throw}
import org.apache.kafka.clients.consumer.{ConsumerRecord, ConsumerRecords}
import org.apache.kafka.common.header.Header
import org.slf4j.LoggerFactory
import scala.collection.JavaConverters._

/**
 * Setup tracing from the Kafka records that were consumed and perform future operations in the
 * provided continuation function `f`. We will only start a trace if one of the [[TraceId]]s in
 * the [[ConsumerRecord]]s was sampled or flagged as debug.
 *
 * @example {{{
 *   val records = consumer.poll(5.seconds.asJava)
 *   KafkaConsumerTracer.trace(records) {
 *     // make more RPCs
 *   }
 * }}}
 */
object KafkaConsumerTracer {

  /**
   * Trace annotation for the traceId of the publisher that published the record. This is present as
   * a [[com.twitter.finagle.tracing.Annotation.BinaryAnnotation BinaryAnnotation]] in a trace.
   */
  val ProducerTraceIdAnnotation = "clnt/kafka.producer.traceId"

  private[this] val KafkaConsumerTraceAnnotators: Seq[KafkaConsumerTraceAnnotator] =
    LoadService[KafkaConsumerTraceAnnotator]()

  private[this] val TraceIdSampled = Some(true)

  private[this] val TraceIdIsDebug = (traceId: TraceId) => traceId.flags.isDebug

  private[this] val logger = Logger(LoggerFactory.getLogger(getClass))

  /**
   * Setup tracing from the Kafka records that were consumed and perform future operations in the
   * provided continuation function `f`. We will only start a trace if one of the [[TraceId]]s in
   * the [[ConsumerRecord]]s was [[TraceIdSampled sampled]] or flagged as [[TraceIdIsDebug debug]].
   *
   * @example {{{
   *   val records = consumer.poll(5.seconds.asJava)
   *   FinagleKafkaConsumerTracer.trace(records) {
   *     // make more RPCs
   *   }
   * }}}
   * @param records consumed from the Kafka consumer.
   * @param configMap configuration used for the Kafka consumer. Defaults to [[Map.empty]].
   *                  This is required to record the following annotations,
   *                  <ul>
   *                    <li>[[KafkaConsumerConfig.FinagleDestKey]]</li>
   *                    <li>[[org.apache.kafka.clients.consumer.ConsumerConfig.CLIENT_ID_CONFIG]]</li>
   *                    <li>[[org.apache.kafka.clients.consumer.ConsumerConfig.GROUP_ID_CONFIG]]</li>
   *                  </ul>
   * @param f continuation function to perform operations on the consumed records. The function will
   *          have tracing setup properly so any RPC calls made will be traced appropriately.
   * @tparam K type of Key.
   * @tparam V type of Value.
   * @tparam T return type of the continuation function `f`.
   * @return the return value from the continuation function `f`.
   */
  def trace[K, V, T](
    records: ConsumerRecords[K, V],
    configMap: Map[String, AnyRef] = Map.empty
  )(
    f: => T
  ): T = {
    if (!records.isEmpty && consumerTracingEnabled()) {
      // Will only trace if the producer trace id is sampled or has debug flag set to true
      val scalaRecords: Iterable[ConsumerRecord[K, V]] = records.asScala
      val producerTraceIds = extractSampledProducerTraceIds(scalaRecords)
      val isSampled = producerTraceIds.nonEmpty
      val isDebug = producerTraceIds.exists(TraceIdIsDebug)
      withTracing(isSampled, isDebug) {
        val trace = Trace()
        if (trace.isActivelyTracing) {
          logger.debug(s"Tracing consumer records with trace id: ${trace.id}")
          KafkaConsumerTraceAnnotators.foreach(_.recordAnnotations(trace, records, configMap))
          logger.debug(s"Recording producer trace ids: $producerTraceIds")
          producerTraceIds.foreach(id => trace.recordBinary(ProducerTraceIdAnnotation, id.toString))
        }
        f
      }
    } else {
      f
    }
  }

  /**
   * A variant of the [[trace]] method that supports asynchronous continuation function using
   * [[Future]].
   *
   * @see [[trace]].
   */
  def traceFuture[K, V, T](
    records: ConsumerRecords[K, V],
    configMap: Map[String, AnyRef] = Map.empty
  )(
    f: => Future[T]
  ): Future[T] = {
    trace(records, configMap)(f)
  }

  private[this] def withTracing[T](sampled: Boolean, isDebug: Boolean)(f: => T): T = {
    // NOTE: This code can be called from a non-finagle controlled thread and hence the
    // Trace.tracers can be empty as it's absent from Contexts.local
    val tracer = if (Trace.tracers.isEmpty) DefaultTracer.self else BroadcastTracer(Trace.tracers)
    val nextTraceId: TraceId = {
      val nextId = Trace.nextId.copy(_sampled = Some(sampled))
      if (isDebug) nextId.copy(flags = nextId.flags.setDebug)
      else nextId
    }
    Trace.letTracerAndId(tracer, nextTraceId)(f)
  }

  private[this] def extractSampledProducerTraceIds[K, V](
    records: Iterable[ConsumerRecord[K, V]]
  ): Set[TraceId] = {
    val traceIdHeaders = records.flatMap(extractTraceIdHeaders)
    val traceIds = traceIdHeaders.flatMap(deserializeTraceId)
    traceIds.toSet
  }

  private[this] def deserializeTraceId(header: Header): Option[TraceId] = {
    TraceId.deserialize(header.value()) match {
      case Return(traceId) if (traceId.sampled == TraceIdSampled) || TraceIdIsDebug(traceId) =>
        Some(traceId)

      case Return(_) => None

      case Throw(ex) =>
        logger.warn(s"Unable to deserialize trace id from header: $header", ex)
        None
    }
  }

  private[this] def extractTraceIdHeaders[K, V](record: ConsumerRecord[K, V]): Iterable[Header] = {
    record.headers().headers(TraceIdHeader).asScala
  }
}
