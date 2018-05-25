package com.twitter.finatra.logback.appender

import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.ConsoleAppender

class TestConsoleAppender(queue: java.util.concurrent.BlockingQueue[ILoggingEvent])
  extends ConsoleAppender[ILoggingEvent] {

  override def doAppend(eventObject: ILoggingEvent): Unit = {
    queue.put(eventObject)
  }

  override def append(eventObject: ILoggingEvent): Unit = {
    // Do nothing
  }
}
