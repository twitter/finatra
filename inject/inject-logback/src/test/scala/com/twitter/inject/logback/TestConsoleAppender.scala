package com.twitter.inject.logback

import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.ConsoleAppender

/** A Test [[ConsoleAppender]] which collects events to the given blocking queue */
class TestConsoleAppender(queue: java.util.concurrent.BlockingQueue[ILoggingEvent])
    extends ConsoleAppender[ILoggingEvent] {

  override def doAppend(eventObject: ILoggingEvent): Unit = {
    queue.put(eventObject)
  }

  override def append(eventObject: ILoggingEvent): Unit = {
    // Do nothing
  }
}
