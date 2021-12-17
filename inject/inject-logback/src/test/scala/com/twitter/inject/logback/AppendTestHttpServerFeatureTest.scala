package com.twitter.inject.logback

import ch.qos.logback.classic.layout.TTLLLayout
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.classic.BasicConfigurator
import ch.qos.logback.classic.Level
import ch.qos.logback.classic.LoggerContext
import ch.qos.logback.core.encoder.LayoutWrappingEncoder
import ch.qos.logback.core.ConsoleAppender
import ch.qos.logback.core.LogbackAsyncAppenderBase
import ch.qos.logback.core.TestLogbackAsyncAppender
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.fasterxml.jackson.module.scala.ScalaObjectMapper
import com.twitter.finagle.http.Request
import com.twitter.finagle.http.Status
import com.twitter.finagle.stats.InMemoryStatsReceiver
import com.twitter.finatra.http.routing.HttpRouter
import com.twitter.finatra.http.Controller
import com.twitter.finatra.http.EmbeddedHttpServer
import com.twitter.finatra.http.HttpServer
import com.twitter.inject.Logging
import com.twitter.inject.Test
import java.util.concurrent.LinkedBlockingQueue
import org.slf4j.LoggerFactory
import scala.collection.JavaConverters._

private object AppendTestHttpServerFeatureTest {
  class TestConfigurator(
    inMemoryStatsReceiver: InMemoryStatsReceiver,
    asyncAppender: LogbackAsyncAppenderBase)
      extends BasicConfigurator {

    override def configure(lc: LoggerContext): Unit = {
      setContext(lc)
      this.addInfo("Setting up test configuration.")

      val nettyLogger = lc.getLogger("io.netty")
      nettyLogger.setLevel(Level.OFF)

      val rootLogger = lc.getLogger("ROOT")
      rootLogger.setLevel(Level.ALL)
      rootLogger.addAppender(asyncAppender)
    }
  }

  class AppendTestHttpServer extends HttpServer with Logging {
    override protected def configureHttp(router: HttpRouter): Unit = {
      router.add(new Controller {
        get("/log_events") { _: Request =>
          warn("Logging initial warning")

          for (x <- 1 to 5) {
            info(s"Logging looped info - $x")

            trace(s"Logging looped trace - $x")

            debug(s"Logging looped debug - $x")

            warn(s"Logging looped warning - $x")

            error(s"Logging looped error - $x")
          }

          response.ok("pong")
        }
      })
    }
  }
}

class AppendTestHttpServerFeatureTest extends Test {
  import AppendTestHttpServerFeatureTest._

  test("Assert Registry entries correctly added") {
    val server = new EmbeddedHttpServer(
      twitterServer = new AppendTestHttpServer,
      disableTestLogging = true
    )

    val inMemoryStatsReceiver: InMemoryStatsReceiver = server.inMemoryStatsReceiver
    val loggerCtx: LoggerContext = LoggerFactory.getILoggerFactory.asInstanceOf[LoggerContext]
    testWithAppender(inMemoryStatsReceiver, loggerCtx) { appender: TestLogbackAsyncAppender =>
      server.assertHealthy()

      val mapper = new ObjectMapper() with ScalaObjectMapper
      mapper.registerModule(DefaultScalaModule)

      val response = server.httpGetAdmin("/admin/registry.json", andExpect = Status.Ok)
      val json: Map[String, Any] =
        mapper.readValue(response.contentString, classOf[Map[String, Any]])

      val registry = json("registry").asInstanceOf[Map[String, Any]]
      registry.contains("library") should be(true)
      registry("library").asInstanceOf[Map[String, String]].contains("logback") should be(true)

      val logback = registry("library")
        .asInstanceOf[Map[String, Any]]("logback")
        .asInstanceOf[Map[String, Any]]
      val testAppenderEntry =
        logback(appender.getName.toLowerCase).asInstanceOf[Map[String, String]]

      testAppenderEntry.size should be(6)
      testAppenderEntry("max_flush_time") should be("5")
      testAppenderEntry("include_caller_data") should be("false")
      testAppenderEntry("max_queue_size") should be("1")
      testAppenderEntry("never_block") should be("false")
      val appenders = appender.iteratorForAppenders().asScala.toSeq
      testAppenderEntry("appenders") should be(appenders.map(_.getName).mkString(","))
      testAppenderEntry("discarding_threshold") should be("2")

      server.close()
    }
  }

  // AsyncAppender drops TRACE/DEBUG/INFO if the queue is 80% full
  test("Assert dropped TRACE, DEBUG, and INFO events are tracked by the InMemoryStatsReceiver") {
    val server = new EmbeddedHttpServer(
      twitterServer = new AppendTestHttpServer,
      disableTestLogging = true
    )

    try {
      val inMemoryStatsReceiver: InMemoryStatsReceiver = server.inMemoryStatsReceiver
      val loggerCtx: LoggerContext = LoggerFactory.getILoggerFactory.asInstanceOf[LoggerContext]
      testWithAppender(inMemoryStatsReceiver, loggerCtx) { appender: TestLogbackAsyncAppender =>
        val appenderStatName = appender.getName.toLowerCase

        server.assertHealthy()

        server.inMemoryStats.gauges.assert(
          s"logback/appender/$appenderStatName/discard/threshold",
          appender.getDiscardingThreshold)
        server.inMemoryStats.gauges
          .assert(s"logback/appender/$appenderStatName/max_flush_time", appender.getMaxFlushTime)
        server.inMemoryStats.gauges
          .assert(s"logback/appender/$appenderStatName/queue_size", appender.getQueueSize)

        server.inMemoryStatsReceiver.clear()
        server.httpGet("/log_events")

        server.inMemoryStats.counters
          .assert(s"logback/appender/$appenderStatName/events/discarded/debug", 5)
        server.inMemoryStats.counters
          .assert(s"logback/appender/$appenderStatName/events/discarded/trace", 5)

        // The server sometimes logs extra messages non-deterministically. We check >= 5 to ensure
        // entries are appropriately being discarded. DEBUG/TRACE entries are dropped determistically
        // so we are sure the dropped entries correlate with the log entries emitted by the http server.
        // If this changes, the test should be restructured to ensure the dropped messages correlate
        // more appropriately.

        // this is sometimes 5 and sometimes 6 because startup sometimes logs a message that
        // gets discarded in this test. however, it's non-deterministic.
        val infoCount = server.inMemoryStats.counters
          .toSortedMap(s"logback/appender/$appenderStatName/events/discarded/info")
        assert(infoCount >= 5)

        server.inMemoryStats.counters
          .get(s"logback/appender/$appenderStatName/events/discarded/warn") should be(None)
        server.inMemoryStats.counters
          .get(s"logback/appender/$appenderStatName/events/discarded/error") should be(None)
      }
    } finally {
      server.close()
    }
  }

  // When setting `neverBlock`, AsyncAppender drops messages regardless of their state to not
  // block the application. In this state, we expect ERROR/WARN to also get dropped.
  test("Assert ERROR AND WARN events are discarded when neverBlock is true") {
    val server = new EmbeddedHttpServer(
      twitterServer = new AppendTestHttpServer,
      disableTestLogging = true
    )

    try {
      val inMemoryStatsReceiver: InMemoryStatsReceiver = server.inMemoryStatsReceiver
      val loggerCtx: LoggerContext = LoggerFactory.getILoggerFactory.asInstanceOf[LoggerContext]
      val consoleAppenderQueue = new java.util.concurrent.LinkedBlockingQueue[ILoggingEvent](1)

      testWithAppender(
        inMemoryStatsReceiver,
        loggerCtx,
        Some(consoleAppenderQueue),
        neverBlock = true
      ) { appender: TestLogbackAsyncAppender =>
        val appenderStatName = appender.getName.toLowerCase

        server.assertHealthy()

        server.inMemoryStats.gauges.assert(
          s"logback/appender/$appenderStatName/discard/threshold",
          appender.getDiscardingThreshold)
        server.inMemoryStats.gauges
          .assert(s"logback/appender/$appenderStatName/max_flush_time", appender.getMaxFlushTime)
        server.inMemoryStats.gauges
          .assert(s"logback/appender/$appenderStatName/queue_size", appender.getQueueSize)

        server.inMemoryStatsReceiver.clear()
        server.httpGet("/log_events")

        server.inMemoryStats.counters
          .assert(s"logback/appender/$appenderStatName/events/discarded/debug", 5)
        server.inMemoryStats.counters
          .assert(s"logback/appender/$appenderStatName/events/discarded/trace", 5)

        // The server sometimes logs extra messages non-deterministically. We check >= 5 to ensure
        // entries are appropriately being discarded. DEBUG/TRACE entries are dropped determistically
        // so we are sure that the dropped entries correlate with the log entries emitted by the http server.
        // If this changes, the test should be restructured to ensure the dropped messages correlate
        // more appropriately.

        // this is sometimes 5 and sometimes 6 because startup sometimes logs a message that
        // gets discarded in this test. however, it's non-deterministic.
        val infoCount = server.inMemoryStats.counters
          .toSortedMap(s"logback/appender/$appenderStatName/events/discarded/info")
        assert(infoCount >= 5)

        // this is sometimes 5 and sometimes 6 because startup sometimes logs a message that
        // gets discarded in this test. however, it's non-deterministic.
        val warnCount = server.inMemoryStats.counters
          .toSortedMap(s"logback/appender/$appenderStatName/events/discarded/warn")
        assert(warnCount >= 5)

        // this is sometimes 5 and sometimes 6 because startup sometimes logs a message that
        // gets discarded in this test. however, it's non-deterministic.
        val errorCount = server.inMemoryStats.counters
          .toSortedMap(s"logback/appender/$appenderStatName/events/discarded/error")
        assert(errorCount >= 5)

        // no events make it to the unit test console appender
        consoleAppenderQueue.size() should equal(0)
      }
    } finally {
      server.close()
    }
  }

  protected def testWithAppender(
    inMemoryStatsReceiver: InMemoryStatsReceiver,
    loggerCtx: LoggerContext,
    consoleAppenderQueueOpt: Option[LinkedBlockingQueue[ILoggingEvent]] = None,
    neverBlock: Boolean = false
  )(
    fn: TestLogbackAsyncAppender => Unit
  ): Unit = {

    /* Stop the current LoggerContext */
    loggerCtx.stop()

    val consoleAppender = getConsoleAppender(loggerCtx, consoleAppenderQueueOpt)
    val asyncAppender: TestLogbackAsyncAppender =
      getAsyncAppender(
        loggerCtx,
        inMemoryStatsReceiver,
        consoleAppender,
        neverBlock
      )

    consoleAppender.start()
    asyncAppender.start()

    setupLoggingConfiguration(loggerCtx, asyncAppender, inMemoryStatsReceiver)
    loggerCtx.start()

    try {
      fn(asyncAppender)
    } finally {
      consoleAppender.stop()
      asyncAppender.stop()
      loggerCtx.stop()
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
    neverBlock: Boolean = false
  ): TestLogbackAsyncAppender = {

    /* if neverBlock is true, we want to stop the async worker thread for testing */
    val appender =
      new TestLogbackAsyncAppender(inMemoryStatsReceiver, stopAsyncWorkerThread = neverBlock)
    appender.setQueueSize(1)
    appender.setDiscardingThreshold(appender.getQueueSize * 2)
    appender.setMaxFlushTime(5) // in millis
    appender.setNeverBlock(neverBlock)
    appender.setContext(loggerCtx)
    appender.setName("TestAsyncAppender")
    appender.addAppender(consoleAppender)
    appender
  }
}
