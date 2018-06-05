package ch.qos.logback.core

import ch.qos.logback.classic.AsyncAppender
import ch.qos.logback.classic.spi.ILoggingEvent
import com.twitter.concurrent.Once
import com.twitter.finagle.stats.{
  Gauge,
  LoadedStatsReceiver,
  StatsReceiver,
  Verbosity,
  VerbosityAdjustingStatsReceiver
}
import com.twitter.util.registry.{GlobalRegistry, Registry}
import java.util.concurrent.ConcurrentLinkedQueue
import scala.collection.JavaConverters._
import scala.util.control.NonFatal

/**
 * This is an [[AsyncAppender]] extension which is located in the `ch.qos.logback.core`
 * package namespace to provide access to the underlying blockingQueue, start, append,
 * and put methods.
 *
 * @note Users are not expected to use this class directly, but instead should use the
 *       [[com.twitter.inject.logback.AsyncAppender]]
 */
abstract class LogbackAsyncAppenderBase(
  statsReceiver: StatsReceiver
) extends AsyncAppender {

  def this() = this(LoadedStatsReceiver)

  private[this] val gauges: ConcurrentLinkedQueue[Gauge] = new ConcurrentLinkedQueue()

  /** Lazy since this.getName() is not set until post-construction */
  private[this] lazy val scopedStatsReceiver: StatsReceiver =
    new VerbosityAdjustingStatsReceiver(
      statsReceiver.scope("logback", "appender", this.getName.toLowerCase()),
      Verbosity.Debug
    )

  private[this] val exportRegistryEntries = Once {
    val appenderName: String = this.getName
    val registry: Registry = GlobalRegistry.get
    val libraryKey = Seq("library", "logback", sanitize(appenderName))

    registry.put(
      libraryKey :+ "max_queue_size",
      this.getQueueSize.toString
    )

    registry.put(
      libraryKey :+ "discarding_threshold",
      this.getDiscardingThreshold.toString
    )

    registry.put(
      libraryKey :+ "include_caller_data",
      this.isIncludeCallerData.toString
    )

    registry.put(
      libraryKey :+ "max_flush_time",
      this.getMaxFlushTime.toString
    )

    registry.put(
      libraryKey :+ "never_block",
      this.isNeverBlock.toString
    )

    registry.put(
      libraryKey ++ Seq("appenders"),
      this.aai // ch.qos.logback.core.spi.AppenderAttachableImpl
        .iteratorForAppenders()
        .asScala
        .map(appender => sanitize(appender.getName)).mkString(","))
  }

  /* Overrides */

  override def start(): Unit = {
    super.start()

    gauges.add(
      scopedStatsReceiver.addGauge("discard/threshold") {
        this.getDiscardingThreshold.toFloat
      }
    )
    gauges.add(
      scopedStatsReceiver.addGauge("queue_size") {
        this.getQueueSize.toFloat
      }
    )
    gauges.add(
      scopedStatsReceiver.addGauge("max_flush_time") {
        this.getMaxFlushTime.toFloat
      }
    )
    gauges.add(
      scopedStatsReceiver.addGauge("current_queue_size") {
        this.getNumberOfElementsInQueue.toFloat
      }
    )
    exportRegistryEntries()
  }

  override def stop(): Unit = {
    gauges.asScala.foreach(_.remove())
    super.stop()
  }

  override def append(eventObject: ILoggingEvent): Unit = {
    if (this.isQueueBelowDiscardingThreshold && this.isDiscardable(eventObject)) {
      scopedStatsReceiver
        .counter(s"events/discarded/${eventObject.getLevel.toString.toLowerCase}")
        .incr()
    } else {
      this.preprocess(eventObject)
      this.put(eventObject)
    }
  }

  /* Private */

  private[this] def isQueueBelowDiscardingThreshold: Boolean =
    this.blockingQueue.remainingCapacity < this.getDiscardingThreshold

  private[this] def put(eventObject: ILoggingEvent): Unit = {
    if (this.neverBlock) {
      if (!this.blockingQueue.offer(eventObject)) {
        scopedStatsReceiver
          .counter(s"events/discarded/${eventObject.getLevel.toString.toLowerCase}")
          .incr()
      }
    } else {
      try {
        this.blockingQueue.put(eventObject)
      } catch {
        case NonFatal(t) =>
          scopedStatsReceiver
            .counter(s"events/discarded/${eventObject.getLevel.toString.toLowerCase}")
            .incr()
          if (t.isInstanceOf[InterruptedException]) Thread.currentThread.interrupt()
      }
    }
  }

  /* sanitize registry keys into snake_case JSON */
  private[this] def sanitize(key: String): String =
    key
      .filter(char => char > 31 && char < 127)
      .toLowerCase
      .replaceAll("-", "_")
      .replaceAll(" ", "_")
}
