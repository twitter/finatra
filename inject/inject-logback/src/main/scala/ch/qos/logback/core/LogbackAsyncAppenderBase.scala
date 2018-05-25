package ch.qos.logback.core

import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.classic.AsyncAppender
import com.twitter.concurrent.Once
import com.twitter.finagle.stats.{
  LoadedStatsReceiver,
  StatsReceiver,
  Verbosity,
  VerbosityAdjustingStatsReceiver
}
import com.twitter.util.registry.GlobalRegistry
import scala.util.control.NonFatal

/**
 * This is an [[AsyncAppender]] extension which is located in the `ch.qos.logback.core`
 * package namespace to provide access to the underlying blockingQueue, start, append,
 * and put methods.
 *
 * @note Users are not expected to use this class directly, but instead should use the
 *       [[com.twitter.inject.logback.AsyncAppender]]
 */
abstract class LogbackAsyncAppenderBase(statsReceiver: StatsReceiver) extends AsyncAppender {
  def this() = this(LoadedStatsReceiver)

  /** Lazy since this.getName() is not set until post-construction */
  private[this] lazy val scopedStatsReceiver: StatsReceiver =
    new VerbosityAdjustingStatsReceiver(
      statsReceiver.scope("logback", "appender", this.getName.toLowerCase()),
      Verbosity.Debug
    )

  private[this] val exportRegistryEntries = Once {
    val appenderName: String = this.getName.toLowerCase()

    GlobalRegistry.get.put(
      Seq("library", "logback", appenderName, "max_queue_size"),
      this.getQueueSize.toString
    )

    GlobalRegistry.get.put(
      Seq("library", "logback", appenderName, "discarding_threshold"),
      this.getDiscardingThreshold.toString
    )

    GlobalRegistry.get.put(
      Seq("library", "logback", appenderName, "include_caller_data"),
      this.isIncludeCallerData.toString
    )

    GlobalRegistry.get.put(
      Seq("library", "logback", appenderName, "max_flush_time"),
      this.getMaxFlushTime.toString
    )

    GlobalRegistry.get.put(
      Seq("library", "logback", appenderName, "never_block"),
      this.isNeverBlock.toString
    )
  }

  /* Overrides */

  override def start(): Unit = {
    scopedStatsReceiver.provideGauge("discard/threshold")(
      this.getDiscardingThreshold
    )
    scopedStatsReceiver.provideGauge("queue_size")(
      this.getQueueSize
    )
    scopedStatsReceiver.provideGauge("max_flush_time")(
      this.getMaxFlushTime
    )
    scopedStatsReceiver.provideGauge("current_queue_size")(
      this.getNumberOfElementsInQueue
    )

    super.start()
    exportRegistryEntries()
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
}
