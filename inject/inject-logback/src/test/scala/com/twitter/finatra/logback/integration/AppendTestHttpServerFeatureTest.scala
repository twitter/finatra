package com.twitter.finatra.logback.integration

import ch.qos.logback.classic.layout.TTLLLayout
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.classic.{BasicConfigurator, Level, LoggerContext}
import ch.qos.logback.core.encoder.LayoutWrappingEncoder
import ch.qos.logback.core.util.StatusPrinter
import ch.qos.logback.core.{ConsoleAppender, LogbackAsyncAppenderBase, TestLogbackAsyncAppenderBase}
import com.twitter.finagle.stats.InMemoryStatsReceiver
import com.twitter.finatra.http.EmbeddedHttpServer
import com.twitter.finatra.logback.appender.TestConsoleAppender
import com.twitter.finatra.logback.integration.server.AppendTestHttpServer
import com.twitter.inject.Test
import org.slf4j.LoggerFactory

class AppendTestHttpServerFeatureTest extends Test {

  test("Assert dropped TRACE, DEBUG, and INFO events are tracked by the InMemoryStatsReceiver") {
    val server = new EmbeddedHttpServer(
      twitterServer = new AppendTestHttpServer
    )

    val inMemoryStatsReceiver: InMemoryStatsReceiver = server.inMemoryStatsReceiver
    var loggerCtx: LoggerContext = LoggerFactory.getILoggerFactory.asInstanceOf[LoggerContext]
    loggerCtx.stop()

    val testConsoleAppender = getConsoleAppender(loggerCtx)
    val testAsyncAppender = getAsyncAppender(
      loggerCtx,
      testConsoleAppender,
      inMemoryStatsReceiver,
      neverBlock = false,
      stopWorker = false
    )

    loggerCtx = setupLoggingConfiguration(loggerCtx, testAsyncAppender, inMemoryStatsReceiver)

    testConsoleAppender.start()
    testAsyncAppender.start()
    loggerCtx.start()

    try {
      server.assertHealthy()

      server.httpGet("/log_events")

      server.assertCounter("logback/appender/testasyncappender/events/discarded/debug", 5)
      server.assertCounter("logback/appender/testasyncappender/events/discarded/info", 5)
      server.assertCounter("logback/appender/testasyncappender/events/discarded/trace", 5)
      server.assertCounter("logback/appender/testasyncappender/events/discarded/warn", 0)
      server.assertCounter("logback/appender/testasyncappender/events/discarded/error", 0)

      server.assertGauge("logback/appender/testasyncappender/discard/threshold", 2)
      server.assertGauge("logback/appender/testasyncappender/max_flush_time", 0)
      server.assertGauge("logback/appender/testasyncappender/queue_size", 1)
    } finally {
      server.close()
      testAsyncAppender.stop()
      testConsoleAppender.stop()
      loggerCtx.stop()
    }
  }

  test("Assert ERROR AND WARN events are discarded when neverBlock is true") {
    val server = new EmbeddedHttpServer(
      twitterServer = new AppendTestHttpServer
    )

    val inMemoryStatsReceiver: InMemoryStatsReceiver = server.inMemoryStatsReceiver
    var loggerCtx: LoggerContext = LoggerFactory.getILoggerFactory.asInstanceOf[LoggerContext]
    loggerCtx.stop()

    val consoleAppenderQueue = new java.util.concurrent.LinkedBlockingQueue[ILoggingEvent](1)

    val testConsoleAppender = getBlockingConsoleAppender(loggerCtx, consoleAppenderQueue)
    val testAsyncAppender =
      getAsyncAppender(
        loggerCtx,
        testConsoleAppender,
        inMemoryStatsReceiver,
        neverBlock = true,
        stopWorker = true
      )
    loggerCtx = setupLoggingConfiguration(loggerCtx, testAsyncAppender, inMemoryStatsReceiver)

    testConsoleAppender.start()
    testAsyncAppender.start()
    loggerCtx.start()

    try {

      server.assertHealthy()

      server.httpGet("/log_events")

      server.assertCounter("logback/appender/testasyncappender/events/discarded/error", 5)
      server.assertCounter("logback/appender/testasyncappender/events/discarded/warn", 5)
      server.assertCounter("logback/appender/testasyncappender/events/discarded/debug", 5)
      server.assertCounter("logback/appender/testasyncappender/events/discarded/info", 5)
      server.assertCounter("logback/appender/testasyncappender/events/discarded/trace", 5)

      server.assertGauge("logback/appender/testasyncappender/discard/threshold", 2)
      server.assertGauge("logback/appender/testasyncappender/max_flush_time", 0)
      server.assertGauge("logback/appender/testasyncappender/queue_size", 1)

      // no events make it to the unit test console appender
      consoleAppenderQueue.size() should equal(0)
    } finally {
      server.close()
      testAsyncAppender.stop()
      testConsoleAppender.stop()
      loggerCtx.stop()
    }
  }

  private def setupLoggingConfiguration(
    loggerCtx: LoggerContext,
    asyncAppender: LogbackAsyncAppenderBase,
    inMemoryStatsReceiver: InMemoryStatsReceiver
  ): LoggerContext = {
    val configurator = new TestConfigurator(inMemoryStatsReceiver, asyncAppender)
    configurator.configure(loggerCtx)
    loggerCtx
  }

  private def getConsoleAppender(lc: LoggerContext): ConsoleAppender[ILoggingEvent] = {
    val ca: ConsoleAppender[ILoggingEvent] = new ConsoleAppender()
    ca.setContext(lc)
    ca.setName("console")
    val encoder = new LayoutWrappingEncoder[ILoggingEvent]
    encoder.setContext(lc)
    val layout = new TTLLLayout
    layout.setContext(lc)
    layout.start()
    encoder.setLayout(layout)
    ca.setEncoder(encoder)
    ca
  }

  private def getBlockingConsoleAppender(
    lc: LoggerContext,
    queue: java.util.concurrent.BlockingQueue[ILoggingEvent]
  ): ConsoleAppender[ILoggingEvent] = {
    val ca: ConsoleAppender[ILoggingEvent] = new TestConsoleAppender(queue)
    ca.setContext(lc)
    ca.setName("console")
    val encoder = new LayoutWrappingEncoder[ILoggingEvent]
    encoder.setContext(lc)
    val layout = new TTLLLayout
    layout.setContext(lc)
    layout.start()
    encoder.setLayout(layout)
    ca.setEncoder(encoder)
    ca
  }

  private def getAsyncAppender(
    lc: LoggerContext,
    ca: ConsoleAppender[ILoggingEvent],
    inMemoryStatsReceiver: InMemoryStatsReceiver,
    neverBlock: Boolean,
    stopWorker: Boolean
  ): LogbackAsyncAppenderBase = {
    val appender = new TestLogbackAsyncAppenderBase(inMemoryStatsReceiver, stopWorker)
    appender.setQueueSize(1)
    appender.setDiscardingThreshold(appender.getQueueSize * 2)
    appender.setMaxFlushTime(0)
    appender.setNeverBlock(neverBlock)
    appender.setContext(lc)
    appender.setName("TestAsyncAppender")
    appender.addAppender(ca)
    appender
  }
}

class TestConfigurator(
  inMemoryStatsReceiver: InMemoryStatsReceiver,
  asyncAppender: LogbackAsyncAppenderBase
) extends BasicConfigurator {

  override def configure(lc: LoggerContext): Unit = {
    setContext(lc)
    this.addInfo("Setting up test configuration.")

    val nettyLogger = lc.getLogger("io.netty")
    nettyLogger.setLevel(Level.OFF)

    val rootLogger = lc.getLogger("ROOT")
    rootLogger.setLevel(Level.ALL)
    rootLogger.addAppender(asyncAppender)

    StatusPrinter.print(lc)
  }
}
