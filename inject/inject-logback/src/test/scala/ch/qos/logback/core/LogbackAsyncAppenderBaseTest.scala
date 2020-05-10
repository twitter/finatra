package ch.qos.logback.core

import com.twitter.finagle.stats.{InMemoryStatsReceiver, StatsReceiver}
import com.twitter.inject.Test

class LogbackAsyncAppenderBaseTest extends Test {

  case class TestAppender(statsReceiver: StatsReceiver)
      extends LogbackAsyncAppenderBase(statsReceiver)

  test(
    "LogbackAsyncAppenderBaseTest#start should setup current_queue_size gauge properly when no appenders") {
    val sr = new InMemoryStatsReceiver
    val appender = TestAppender(sr)
    appender.setName("test")

    // Calling 'start' registers gauges in the stats receiver. One of these
    // gauges gets a value from the underlying 'blockingQueue'. The
    // 'blockingQueue' can unfortunately be null, as the base class's 'start'
    // method will short circuit if the 'appenderCount' is 0 or the 'queueSize'
    // is less than 1. The 'blockingQueue' is used to retrieve the 'current_queue_size'
    // gauge, so we verify here that the gauge works even when the 'blockingQueue' is
    // null.

    assert(appender.blockingQueue == null)
    assert(sr.gauges.size == 0)

    appender.start()

    assert(appender.blockingQueue == null)
    assert(sr.gauges.size == 4)

    assert(sr.gauges(Seq("logback", "appender", "test", "current_queue_size"))() == 0)

    appender.stop()
  }

}
