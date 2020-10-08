package com.twitter.finatra.kafkastreams.flushing

import com.twitter.finagle.stats.{Stat, StatsReceiver, Verbosity}
import com.twitter.finatra.kafkastreams.internal.utils.ProcessorContextLogging
import com.twitter.util.{Duration, Future}
import java.util.concurrent.ConcurrentLinkedQueue
import org.apache.kafka.streams.processor.internals.{
  InternalProcessorContext,
  ProcessorRecordContext
}
import org.apache.kafka.streams.processor.{
  Cancellable,
  ProcessorContext,
  PunctuationType,
  Punctuator
}

/**
 * The AsyncTransformer trait allows async futures to be used to emit records downstreams.
 *
 * @see [[https://issues.apache.org/jira/browse/KAFKA-6989]] for related ticket in Kafka Streams backlog
 * @see [[https://stackoverflow.com/questions/42049047/how-to-handle-error-and-dont-commit-when-use-kafka-streams-dsl/42056286#comment92197161_42056286]]
 * @see [[https://stackoverflow.com/questions/42064430/external-system-queries-during-kafka-stream-processing]]
 * @see [[https://issues.apache.org/jira/browse/KAFKA-7432]]
 *
 * @note Completed Futures add output records to an outstandingResults set. Future's do not directly
 *       call `context.forward` on success since the Processor/Transformer classes have a defined
 *       lifecycle which revolves around 2 main processing methods (process/punctuate or
 *       transform/punctuate).
 *       Kafka Streams then ensures that these methods are never called from 2 threads at the same time.
 *       Kafka Streams assumes "forward" would only ever be called from the thread that calls
 *       process/transform/punctuate. As such, it could be dangerous to have a Finagle thread
 *       calling forward at any time. In addition, ProcessorRecordContext associated with each input
 *       record is stored in memory and reattached to the output record before flushing. This
 *       ensures fields such as headers are preserved.
 *
 * @note `throwIfAsyncFailure` is used to fail the Kafka Streams service more quickly than waiting
 *       for an eventual failure to occur at the next commit interval. We try to fail fast and a
 *       future failure will result in your entire instance shutting down. This default behavior
 *       prevents data loss. If you want your service to handle failed futures please use
 *       handle/transform on your returned future.
 *
 * @param statsReceiver Receiver for AsyncTransformer metrics. Scope the receiver to differentiate
 *                      metrics between different AsyncTransformers in the topology.
 */
abstract class AsyncTransformer[K1, V1, K2, V2](
  override val statsReceiver: StatsReceiver,
  override val maxOutstandingFuturesPerTask: Int,
  flushAsyncRecordsInterval: Duration,
  override val commitInterval: Duration,
  override val flushTimeout: Duration)
    extends FlushingTransformer[K1, V1, K2, V2]
    with AsyncFlushing[K1, V1, K2, V2]
    with ProcessorContextLogging {

  @volatile private var flushOutputRecordsCancellable: Cancellable = _

  private val outstandingResults = new ConcurrentLinkedQueue[(K2, V2, ProcessorRecordContext)]

  private val outstandingResultsGauge =
    statsReceiver.addGauge(Verbosity.Debug, "outstandingResults")(numOutstandingResults)

  private var _context: ProcessorContext = _
  private var internalProcessorContext: InternalProcessorContext = _

  override protected def processorContext: ProcessorContext = _context

  private[this] val latencyStat = statsReceiver.stat("transform_async_latency_ms")

  /* Abstract */

  /**
   * Asynchronously transform the record with the given key and value.
   *
   * Additionally, any {@link StateStore state} that is {@link KStream#transform(TransformerSupplier, String...)
   * attached} to this operator can be accessed and modified arbitrarily (cf. {@link ProcessorContext#getStateStore(String)}).
   *
   * @param key   the key for the record
   * @param value the value for the record
   * @param recordContext the context for the record containing metadata such as the topic name,
   *                      partition number, timestamp, and headers. The timestamp and headers are
   *                      mutable and will be passed downstream.
   *
   * @return Future iterable of output messages each containing a key, value, and the recordContext
   */
  protected def transformAsync(
    key: K1,
    value: V1,
    recordContext: ProcessorRecordContext
  ): Future[Iterable[(K2, V2, ProcessorRecordContext)]]

  /* Overrides */

  /**
   * Initialize the transformer with processor context and schedule a wall-clock based punctuator
   * for flushing the buffered output records.
   *
   * @note this method may be called more than once to reuse the Transformer object, so it is important
   *       to clear the states in `close()`.
   * @see [[https://kafka.apache.org/24/javadoc/org/apache/kafka/streams/kstream/Transformer.html#init-org.apache.kafka.streams.processor.ProcessorContext-]]
   */
  final override def init(context: ProcessorContext): Unit = {
    assert(
      context.isInstanceOf[InternalProcessorContext],
      """ProcessorContext is not InternalProcessorContext, we cannot continue due to the need for
        |`InternalProcessorContext.recordContext()`""".stripMargin
    )

    _context = context
    internalProcessorContext = context.asInstanceOf[InternalProcessorContext]

    flushOutputRecordsCancellable = context
      .schedule(
        flushAsyncRecordsInterval.inMillis,
        PunctuationType.WALL_CLOCK_TIME,
        new Punctuator {
          override def punctuate(timestamp: Long): Unit = {
            flushOutputRecords()
          }
        }
      )

    super.onInit()
  }

  override final def transform(key: K1, value: V1): (K2, V2) = {
    val recordContext: ProcessorRecordContext = internalProcessorContext.recordContext()

    addFuture(
      key,
      value,
      Stat.timeFuture(latencyStat)(transformAsync(key, value, recordContext))
    )

    null
  }

  override def onFutureSuccess(
    key: K1,
    value: V1,
    result: Iterable[(K2, V2, ProcessorRecordContext)]
  ): Unit = {
    for ((transformedKey, transformedValue, recordContext) <- result) {
      outstandingResults.add((transformedKey, transformedValue, recordContext))
    }
  }

  override def onFlush(): Unit = {
    //First call super.onFlush so that we wait for outstanding futures to complete
    super.onFlush()

    //Then output any resulting records
    flushOutputRecords()
  }

  final override def close(): Unit = {
    debug("Close")

    if (flushOutputRecordsCancellable != null) {
      flushOutputRecordsCancellable.cancel()
      flushOutputRecordsCancellable = null
    }

    if (!outstandingResults.isEmpty) {
      error(s"""outstandingResults is not empty when closing AsyncTransformer,
           |size=${outstandingResults.size}.""".stripMargin)
      outstandingResults.clear()
    }

    outstandingResultsGauge.remove()

    super.onClose()
  }

  protected def numOutstandingResults: Int = outstandingResults.size()

  /* Private */

  private def flushOutputRecords(): Unit = {
    // first save the current context
    val preflushingRecordContext: ProcessorRecordContext = internalProcessorContext.recordContext()

    var ele = outstandingResults.poll()
    while (ele != null) {
      val (key, value, recordContext) = ele

      // set the RecordContext to the one associated with the original message
      internalProcessorContext.setRecordContext(recordContext)

      processorContext.forward(key, value)

      ele = outstandingResults.poll()
    }

    // restore to the previous pre-flushing context
    internalProcessorContext.setRecordContext(preflushingRecordContext)
  }
}
