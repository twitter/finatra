package com.twitter.inject.logback

import ch.qos.logback.classic.{Level, Logger, LoggerContext}
import ch.qos.logback.classic.layout.TTLLLayout
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.{ConsoleAppender, LogbackAsyncAppenderBase, TestLogbackAsyncAppender}
import ch.qos.logback.core.encoder.LayoutWrappingEncoder
import com.twitter.finagle.stats.InMemoryStatsReceiver
import com.twitter.inject.logback.AppendTestHttpServerFeatureTest.TestConfigurator
import java.util.concurrent.LinkedBlockingQueue

trait LoggingTest {

  protected def testWithLogger(
    inMemoryStatsReceiver: InMemoryStatsReceiver,
    loggerCtx: LoggerContext,
    consoleAppenderQueueOpt: Option[LinkedBlockingQueue[ILoggingEvent]] = None,
    neverBlock: Boolean = false,
    stopAsyncWorkerThread: Boolean = false
  )(fn: (Logger, TestLogbackAsyncAppender) => Unit): Unit = {

    val appender: TestLogbackAsyncAppender = getAsyncAppender(
      loggerCtx,
      inMemoryStatsReceiver,
      getConsoleAppender(loggerCtx, consoleAppenderQueueOpt),
      neverBlock,
      stopAsyncWorkerThread
    )
    appender.start()

    val logger: Logger = loggerCtx.getLogger("testLogger")
    logger.setLevel(Level.ALL)
    logger.addAppender(appender)

    try {
      fn(logger, appender)
    } finally {
      appender.stop()
    }
  }

  protected def testWithAsyncAppender(
    inMemoryStatsReceiver: InMemoryStatsReceiver,
    loggerCtx: LoggerContext,
    consoleAppenderQueueOpt: Option[LinkedBlockingQueue[ILoggingEvent]] = None,
    neverBlock: Boolean = false,
    stopAsyncWorkerThread: Boolean = false
  )(fn: (TestLogbackAsyncAppender) => Unit)(onClose: => Unit): Unit = {

    /* Stop the current LoggerContext */
    loggerCtx.stop()

    val consoleAppender = getConsoleAppender(loggerCtx, consoleAppenderQueueOpt)
    val asyncAppender: TestLogbackAsyncAppender =
      getAsyncAppender(
        loggerCtx,
        inMemoryStatsReceiver,
        consoleAppender,
        neverBlock,
        stopAsyncWorkerThread
      )

    consoleAppender.start()
    asyncAppender.start()

    setupLoggingConfiguration(loggerCtx, asyncAppender, inMemoryStatsReceiver)
    loggerCtx.start()

    try {
      fn(asyncAppender)
    } finally {
      asyncAppender.stop()
      loggerCtx.stop()
      onClose
    }
  }

  private[this] def setupLoggingConfiguration(
    loggerCtx: LoggerContext,
    asyncAppender: LogbackAsyncAppenderBase,
    inMemoryStatsReceiver: InMemoryStatsReceiver
  ): Unit = {
    val configurator = new TestConfigurator(inMemoryStatsReceiver, asyncAppender)
    configurator.configure(loggerCtx)
  }

  private[this] def getConsoleAppender(
    loggerCtx: LoggerContext,
    queueOpt: Option[java.util.concurrent.BlockingQueue[ILoggingEvent]] = None
  ): ConsoleAppender[ILoggingEvent] = {

    val consoleAppender: ConsoleAppender[ILoggingEvent] = queueOpt match {
      case Some(queue) =>
        new TestConsoleAppender(queue)
      case _ =>
        new ConsoleAppender()
    }
    consoleAppender.setContext(loggerCtx)
    consoleAppender.setName("console")
    val encoder = new LayoutWrappingEncoder[ILoggingEvent]
    encoder.setContext(loggerCtx)
    val layout = new TTLLLayout
    layout.setContext(loggerCtx)
    layout.start()
    encoder.setLayout(layout)
    consoleAppender.setEncoder(encoder)
    consoleAppender
  }

  private[this] def getAsyncAppender(
    loggerCtx: LoggerContext,
    inMemoryStatsReceiver: InMemoryStatsReceiver,
    consoleAppender: ConsoleAppender[ILoggingEvent],
    neverBlock: Boolean = false,
    stopAsyncWorkerThread: Boolean = false
  ): TestLogbackAsyncAppender = {

    val appender = new TestLogbackAsyncAppender(inMemoryStatsReceiver, stopAsyncWorkerThread)
    appender.setQueueSize(1)
    appender.setDiscardingThreshold(appender.getQueueSize * 2)
    appender.setMaxFlushTime(0)
    appender.setNeverBlock(neverBlock)
    appender.setContext(loggerCtx)
    appender.setName("TestAsyncAppender")
    appender.addAppender(consoleAppender)
    appender
  }
}
