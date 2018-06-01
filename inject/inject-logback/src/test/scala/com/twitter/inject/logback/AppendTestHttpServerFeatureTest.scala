package com.twitter.inject.logback

import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.classic.{BasicConfigurator, Level, LoggerContext}
import ch.qos.logback.core.util.StatusPrinter
import ch.qos.logback.core.{LogbackAsyncAppenderBase, TestLogbackAsyncAppender}
import com.twitter.finagle.http.{Request, Status}
import com.twitter.finagle.stats.InMemoryStatsReceiver
import com.twitter.finatra.http.routing.HttpRouter
import com.twitter.finatra.http.{Controller, EmbeddedHttpServer, HttpServer}
import com.twitter.inject.logback.AppendTestHttpServerFeatureTest._
import com.twitter.inject.{Logging, Test}
import org.slf4j.LoggerFactory
import scala.collection.JavaConverters._
import scala.util.parsing.json.JSON

object AppendTestHttpServerFeatureTest {

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

class AppendTestHttpServerFeatureTest extends Test with LoggingTest {

  test("Assert Registry entries correctly added") {
    val server = new EmbeddedHttpServer(
      twitterServer = new AppendTestHttpServer,
      disableTestLogging = true
    )

    val loggerCtx: LoggerContext = LoggerFactory.getILoggerFactory.asInstanceOf[LoggerContext]
    val inMemoryStatsReceiver: InMemoryStatsReceiver = server.inMemoryStatsReceiver

    testWithAsyncAppender(inMemoryStatsReceiver, loggerCtx) { appender: TestLogbackAsyncAppender =>
      server.assertHealthy()

      val response = server.httpGetAdmin("/admin/registry.json", andExpect = Status.Ok)
      val json: Map[String, Any] =
        JSON.parseFull(response.contentString).get.asInstanceOf[Map[String, Any]]

      val registry = json("registry").asInstanceOf[Map[String, Any]]
      registry.contains("library") should be(true)
      registry("library").asInstanceOf[Map[String, String]].contains("logback") should be(true)

      val logback = registry("library")
        .asInstanceOf[Map[String, Any]]("logback")
        .asInstanceOf[Map[String, Any]]
      val testAppenderEntry =
        logback(appender.getName.toLowerCase).asInstanceOf[Map[String, String]]

      testAppenderEntry.size should be(6)
      testAppenderEntry("max_flush_time") should be("0")
      testAppenderEntry("include_caller_data") should be("false")
      testAppenderEntry("max_queue_size") should be("1")
      testAppenderEntry("never_block") should be("false")
      val appenders = appender.iteratorForAppenders().asScala.toSeq
      testAppenderEntry("appenders") should be(appenders.map(_.getName).mkString(","))
      testAppenderEntry("discarding_threshold") should be("2")
    } {
      server.close()
    }
  }

  test("Assert dropped TRACE, DEBUG, and INFO events are tracked by the InMemoryStatsReceiver") {
    val server = new EmbeddedHttpServer(
      twitterServer = new AppendTestHttpServer,
      disableTestLogging = true
    )

    val loggerCtx: LoggerContext = LoggerFactory.getILoggerFactory.asInstanceOf[LoggerContext]
    val inMemoryStatsReceiver: InMemoryStatsReceiver = server.inMemoryStatsReceiver

    testWithAsyncAppender(inMemoryStatsReceiver, loggerCtx) { appender: TestLogbackAsyncAppender =>
      val appenderStatName = appender.getName.toLowerCase

      server.assertHealthy()
      server.httpGet("/log_events")

      server.assertCounter(s"logback/appender/$appenderStatName/events/discarded/debug", 5)
      server.assertCounter(s"logback/appender/$appenderStatName/events/discarded/info", 5)
      server.assertCounter(s"logback/appender/$appenderStatName/events/discarded/trace", 5)
      server.assertCounter(s"logback/appender/$appenderStatName/events/discarded/warn", 0)
      server.assertCounter(s"logback/appender/$appenderStatName/events/discarded/error", 0)

      server.assertGauge(s"logback/appender/$appenderStatName/discard/threshold", 2)
      server.assertGauge(s"logback/appender/$appenderStatName/max_flush_time", 0)
      server.assertGauge(s"logback/appender/$appenderStatName/queue_size", 1)
    } {
      server.close()
    }
  }

  test("Assert ERROR AND WARN events are discarded when neverBlock is true") {
    val server = new EmbeddedHttpServer(
      twitterServer = new AppendTestHttpServer,
      disableTestLogging = true
    )

    val loggerCtx: LoggerContext = LoggerFactory.getILoggerFactory.asInstanceOf[LoggerContext]
    val inMemoryStatsReceiver: InMemoryStatsReceiver = server.inMemoryStatsReceiver
    val consoleAppenderQueue = new java.util.concurrent.LinkedBlockingQueue[ILoggingEvent](1)

    testWithAsyncAppender(
      inMemoryStatsReceiver,
      loggerCtx,
      Some(consoleAppenderQueue),
      neverBlock = true,
      stopAsyncWorkerThread = true
    ) { appender: TestLogbackAsyncAppender =>
      val appenderStatName = appender.getName.toLowerCase

      server.assertHealthy()
      server.httpGet("/log_events")

      server.assertCounter(s"logback/appender/$appenderStatName/events/discarded/error", 5)
      server.assertCounter(s"logback/appender/$appenderStatName/events/discarded/warn", 5)
      server.assertCounter(s"logback/appender/$appenderStatName/events/discarded/debug", 5)
      server.assertCounter(s"logback/appender/$appenderStatName/events/discarded/info", 5)
      server.assertCounter(s"logback/appender/$appenderStatName/events/discarded/trace", 5)

      server.assertGauge(s"logback/appender/$appenderStatName/discard/threshold", 2)
      server.assertGauge(s"logback/appender/$appenderStatName/max_flush_time", 0)
      server.assertGauge(s"logback/appender/$appenderStatName/queue_size", 1)

      // no events make it to the unit test console appender
      consoleAppenderQueue.size() should equal(0)
    } {
      server.close()
    }
  }
}
