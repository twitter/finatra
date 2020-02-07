package com.twitter.finatra.kafkastreams.flushing

import com.twitter.finagle.stats.{Stat, StatsReceiver}
import com.twitter.finatra.kafkastreams.internal.utils.ProcessorContextLogging
import com.twitter.finatra.kafkastreams.utils.MessageTimestamp
import com.twitter.util.{Duration, Future}
import java.util.concurrent.ConcurrentHashMap
import org.apache.kafka.streams.processor.{Cancellable, ProcessorContext, PunctuationType, Punctuator, To}

/**
 * The AsyncTransformer trait allows async futures to be used to emit records downstreams
 *
 * See https://issues.apache.org/jira/browse/KAFKA-6989 for related ticket in Kafka Streams backlog
 * See also:
 * https://stackoverflow.com/questions/42049047/how-to-handle-error-and-dont-commit-when-use-kafka-streams-dsl/42056286#comment92197161_42056286
 * https://stackoverflow.com/questions/42064430/external-system-queries-during-kafka-stream-processing?noredirect=1&lq=1
 * https://issues.apache.org/jira/browse/KAFKA-7432
 *
 * Note: Completed futures add output records to an outstandingResults set. Future's do not directly
 * call context.forward on success since the Processor/Transformer classes have a defined lifecycle
 * which revolve around 2 main processing methods (process/punctuate or transform/punctuate).
 * Kafka Streams then ensures that these methods are never called from 2 threads at the same time.
 * Kafka Streams assumes "forward" would only ever be called from the thread that calls process/transform/punctuate.
 * As such, it could be dangerous to have a Finagle thread calling forward at any time
 *
 * Note 2: throwIfAsyncFailure is used to fail the Kafka Streams service more quickly than waiting
 * for an eventual failure to occur at the next commit interval. We try to fail fast and a future failure
 * will result in your entire instance shutting down. This default behavior prevents data loss. If
 * you want your service to handle failed futures please use handle/transform on your returned future
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
  private val outstandingResults = ConcurrentHashMap
    .newKeySet[(K2, V2, MessageTimestamp)](maxOutstandingFuturesPerTask)

  private var _context: ProcessorContext = _

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
   * @param timestamp the timestamp for the record
   *
   * @return Future iterable of output messages each containing a key, value, and message timestamp
   */
  protected def transformAsync(
    key: K1,
    value: V1,
    timestamp: MessageTimestamp
  ): Future[Iterable[(K2, V2, Long)]]

  /* Overrides */

  final override def init(context: ProcessorContext): Unit = {
    _context = context

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
    addFuture(
      key,
      value,
      Stat.timeFuture(latencyStat)(transformAsync(key, value, _context.timestamp()))
    )

    null
  }

  override protected def onFutureSuccess(
    key: K1,
    value: V1,
    result: Iterable[(K2, V2, MessageTimestamp)]
  ): Unit = {
    for ((key, value, timestamp) <- result) {
      outstandingResults.add((key, value, timestamp))
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

    super.onClose()
  }

  /* Private */

  private def flushOutputRecords(): Unit = {
    val iterator = outstandingResults.iterator()
    while (iterator.hasNext) {
      val (key, value, timestamp) = iterator.next()

      processorContext.forward(key, value, To.all().withTimestamp(timestamp))

      iterator.remove()
    }
  }
}
