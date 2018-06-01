package com.twitter.inject.logback

import ch.qos.logback.classic.LoggerContext
import ch.qos.logback.classic.spi.ILoggingEvent
import com.twitter.finagle.stats.InMemoryStatsReceiver
import com.twitter.inject.Test
import com.twitter.util.registry.{Entry, GlobalRegistry, SimpleRegistry}
import scala.collection.JavaConverters._

class LogbackAsyncAppenderBaseTest extends Test with LoggingTest {

  test("Registry entries are added correctly") {
    GlobalRegistry.withRegistry(new SimpleRegistry) {
      val inMemoryStatsReceiver = new InMemoryStatsReceiver
      val loggerCtx = new LoggerContext()

      testWithLogger(inMemoryStatsReceiver, loggerCtx) {
        case (_, appender) =>
          val registry = GlobalRegistry.get
          val entries: Seq[Entry] = registry.iterator.toSeq
          entries.size should equal(6)
          entries.foreach { entry =>
            val key = entry.key.mkString("/")
            key should startWith(s"library/logback/${appender.getName.toLowerCase}")
            if (key.endsWith("max_flush_time")) {
              entry.value should be("0")
            } else if (key.endsWith("include_caller_data")) {
              entry.value should be("false")
            } else if (key.endsWith("max_queue_size")) {
              entry.value should be("1")
            } else if (key.endsWith("never_block")) {
              entry.value should be("false")
            } else if (key.endsWith("appenders")) {
              val appenders = appender.iteratorForAppenders().asScala.toSeq
              entry.value should be(appenders.map(_.getName).mkString(","))
            } else if (key.endsWith("discarding_threshold")) {
              entry.value should be("2")
            } else {
              fail()
            }
          }
      }
    }
  }

  test("Assert dropped TRACE, DEBUG, and INFO events are tracked by InMemoryStatsReceiver") {
    val inMemoryStatsReceiver = new InMemoryStatsReceiver
    val loggerCtx = new LoggerContext()

    testWithLogger(inMemoryStatsReceiver, loggerCtx) {
      case (logger, appender) =>
        for (x <- 1 to 5) {
          logger.info(s"Logging info - $x")
          logger.trace(s"Logging trace - $x")
          logger.debug(s"Logging debug - $x")
          logger.warn(s"Logging warning - $x")
          logger.error(s"Logging error - $x")
        }

        assertCounter(
          inMemoryStatsReceiver,
          Seq("logback", "appender", "testasyncappender", "events/discarded/debug"),
          5
        )
        assertCounter(
          inMemoryStatsReceiver,
          Seq("logback", "appender", "testasyncappender", "events/discarded/trace"),
          5
        )
        assertCounter(
          inMemoryStatsReceiver,
          Seq("logback", "appender", "testasyncappender", "events/discarded/info"),
          5
        )
        assertCounter(
          inMemoryStatsReceiver,
          Seq("logback", "appender", "testasyncappender", "events/discarded/error"),
          0
        )
        assertCounter(
          inMemoryStatsReceiver,
          Seq("logback", "appender", "testasyncappender", "events/discarded/warn"),
          0
        )

        // Assert Gauges
        assertGauge(
          inMemoryStatsReceiver,
          Seq("logback", "appender", "testasyncappender", "discard/threshold"),
          appender.getQueueSize * 2
        )
        assertGauge(
          inMemoryStatsReceiver,
          Seq("logback", "appender", "testasyncappender", "max_flush_time"),
          appender.getMaxFlushTime
        )
        assertGauge(
          inMemoryStatsReceiver,
          Seq("logback", "appender", "testasyncappender", "queue_size"),
          appender.getQueueSize
        )
        assertGauge(
          inMemoryStatsReceiver,
          Seq("logback", "appender", "testasyncappender", "current_queue_size"),
          appender.getNumberOfElementsInQueue
        )
    }
  }

  test("Assert ERROR AND WARN events are discarded when neverBlock is true") {
    val inMemoryStatsReceiver = new InMemoryStatsReceiver
    val loggerCtx = new LoggerContext()
    val consoleAppenderQueue = new java.util.concurrent.LinkedBlockingQueue[ILoggingEvent](1)

    testWithLogger(
      inMemoryStatsReceiver,
      loggerCtx,
      Some(consoleAppenderQueue),
      neverBlock = true,
      stopAsyncWorkerThread = true
    ) {
      case (logger, appender) =>
        // fills the async appender queue, and is never removed since the async worker thread is stopped.
        logger.warn("Logging initial warning")

        for (x <- 1 to 5) {
          logger.warn(s"Logging looped warning - $x")
          logger.error(s"Logging looped error - $x")
        }

        assertCounter(
          inMemoryStatsReceiver,
          Seq("logback", "appender", "testasyncappender", "events/discarded/error"),
          5
        )
        assertCounter(
          inMemoryStatsReceiver,
          Seq("logback", "appender", "testasyncappender", "events/discarded/warn"),
          5
        )

        // Assert Gauges
        assertGauge(
          inMemoryStatsReceiver,
          Seq("logback", "appender", "testasyncappender", "discard/threshold"),
          appender.getQueueSize * 2
        )
        assertGauge(
          inMemoryStatsReceiver,
          Seq("logback", "appender", "testasyncappender", "max_flush_time"),
          appender.getMaxFlushTime
        )
        assertGauge(
          inMemoryStatsReceiver,
          Seq("logback", "appender", "testasyncappender", "queue_size"),
          appender.getQueueSize
        )
        assertGauge(
          inMemoryStatsReceiver,
          Seq("logback", "appender", "testasyncappender", "current_queue_size"),
          appender.getNumberOfElementsInQueue
        )

        // assert no events make it to the test console appender as the async worker thread is stopped.
        consoleAppenderQueue.size() should equal(0)
    }
  }

  private[this] def assertCounter(
    inMemoryStatsReceiver: InMemoryStatsReceiver,
    name: Seq[String],
    expected: Long
  ): Unit = {
    getCounter(inMemoryStatsReceiver, name) should equal(expected)
  }

  private[this] def getCounter(
    inMemoryStatsReceiver: InMemoryStatsReceiver,
    name: Seq[String]
  ): Long = {
    inMemoryStatsReceiver.counters.getOrElse(name, 0)
  }

  private[this] def getGauge(
    inMemoryStatsReceiver: InMemoryStatsReceiver,
    name: Seq[String]
  ): Float = {
    inMemoryStatsReceiver.gauges.get(name) map { _.apply() } getOrElse 0f
  }

  private[this] def assertGauge(
    inMemoryStatsReceiver: InMemoryStatsReceiver,
    name: Seq[String],
    expected: Float
  ): Unit = {
    val value = getGauge(inMemoryStatsReceiver, name)
    value should equal(expected)
  }
}
