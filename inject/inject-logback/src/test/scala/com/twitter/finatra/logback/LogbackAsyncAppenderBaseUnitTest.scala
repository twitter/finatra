package com.twitter.finatra.logback

import ch.qos.logback.classic.layout.TTLLLayout
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.classic.{Level, Logger, LoggerContext}
import ch.qos.logback.core.encoder.LayoutWrappingEncoder
import ch.qos.logback.core.{ConsoleAppender, TestLogbackAsyncAppenderBase}
import com.twitter.finagle.stats.InMemoryStatsReceiver
import com.twitter.finatra.logback.appender.TestConsoleAppender
import com.twitter.inject.Test

class LogbackAsyncAppenderBaseUnitTest extends Test {

  test("Assert dropped TRACE, DEBUG, and INFO events are tracked by InMemoryStatsReceiver") {
    val inMemoryStatsReceiver = new InMemoryStatsReceiver

    val loggerCtx = new LoggerContext()

    val appender = new TestLogbackAsyncAppenderBase(inMemoryStatsReceiver)
    appender.setQueueSize(1)
    appender.setDiscardingThreshold(appender.getQueueSize * 2)
    appender.setMaxFlushTime(0)
    appender.setContext(loggerCtx)
    appender.setName("testAsyncAppender")

    val UnitTestConsoleAppender: ConsoleAppender[ILoggingEvent] = new ConsoleAppender()
    UnitTestConsoleAppender.setContext(loggerCtx)
    UnitTestConsoleAppender.setName("console")
    val encoder: LayoutWrappingEncoder[ILoggingEvent] = new LayoutWrappingEncoder()
    encoder.setContext(loggerCtx)
    val layout: TTLLLayout = new TTLLLayout
    layout.setContext(loggerCtx)
    layout.start()
    encoder.setLayout(layout)
    UnitTestConsoleAppender.setEncoder(encoder)
    UnitTestConsoleAppender.start()

    appender.addAppender(UnitTestConsoleAppender)
    appender.start()

    val testLogger: Logger = loggerCtx.getLogger("testLogger")
    testLogger.setLevel(Level.ALL)
    testLogger.addAppender(appender)

    try {
      for (x <- 1 to 5) {
        testLogger.info(s"Logging info - $x")

        testLogger.trace(s"Logging trace - $x")

        testLogger.debug(s"Logging debug - $x")

        testLogger.warn(s"Logging warning - $x")

        testLogger.error(s"Logging error - $x")
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

    } finally {
      UnitTestConsoleAppender.stop()
      appender.stop()
    }
  }
  test("Assert ERROR AND WARN events are discarded when neverBlock is true") {
    val inMemoryStatsReceiver = new InMemoryStatsReceiver

    val loggerCtx = new LoggerContext()

    val appender = new TestLogbackAsyncAppenderBase(inMemoryStatsReceiver, stopWorker = true)
    appender.setQueueSize(1)
    appender.setDiscardingThreshold(appender.getQueueSize * 2)
    appender.setMaxFlushTime(0)
    appender.setContext(loggerCtx)
    appender.setNeverBlock(true)
    appender.setName("testAsyncAppender")

    val consoleAppenderQueue = new java.util.concurrent.LinkedBlockingQueue[ILoggingEvent](1)

    val testConsoleAppender: ConsoleAppender[ILoggingEvent] =
      new TestConsoleAppender(consoleAppenderQueue)

    testConsoleAppender.setContext(loggerCtx)
    testConsoleAppender.setName("console")
    val encoder: LayoutWrappingEncoder[ILoggingEvent] = new LayoutWrappingEncoder()
    encoder.setContext(loggerCtx)
    val layout: TTLLLayout = new TTLLLayout
    layout.setContext(loggerCtx)
    layout.start()
    encoder.setLayout(layout)
    testConsoleAppender.setEncoder(encoder)
    testConsoleAppender.start()

    appender.addAppender(testConsoleAppender)
    appender.start()

    val testLogger: Logger = loggerCtx.getLogger("testLogger")
    testLogger.setLevel(Level.ALL)
    testLogger.addAppender(appender)

    try {
      testLogger.warn("Logging initial warning")

      for (x <- 1 to 5) {
        testLogger.warn(s"Logging looped warning - $x")

        testLogger.error(s"Logging looped error - $x")
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

      // no events make it to the unit test console appender
      consoleAppenderQueue.size() should equal(0)

    } finally {
      testConsoleAppender.stop()
      appender.stop()
    }
  }

  def assertCounter(
    inMemoryStatsReceiver: InMemoryStatsReceiver,
    name: Seq[String],
    expected: Long
  ): Unit = {
    getCounter(inMemoryStatsReceiver, name) should equal(expected)
  }

  def getCounter(inMemoryStatsReceiver: InMemoryStatsReceiver, name: Seq[String]): Long = {
    inMemoryStatsReceiver.counters.getOrElse(name, 0)
  }

  def getGauge(inMemoryStatsReceiver: InMemoryStatsReceiver, name: Seq[String]): Float = {
    inMemoryStatsReceiver.gauges.get(name) map { _.apply() } getOrElse 0f
  }

  def assertGauge(
    inMemoryStatsReceiver: InMemoryStatsReceiver,
    name: Seq[String],
    expected: Float
  ): Unit = {
    val value = getGauge(inMemoryStatsReceiver, name)
    value should equal(expected)
  }
}
