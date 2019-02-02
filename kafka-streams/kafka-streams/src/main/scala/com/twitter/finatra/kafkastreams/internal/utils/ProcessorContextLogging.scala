package com.twitter.finatra.kafkastreams.internal.utils

import com.twitter.util.logging.Logger
import org.apache.kafka.streams.processor.ProcessorContext
import org.joda.time.DateTime
import org.joda.time.format.ISODateTimeFormat

//TODO: Change viability to [kafkastreams] after deleting deprecated dependent code
private[finatra] trait ProcessorContextLogging {

  private val _logger = Logger(getClass)

  protected def processorContext: ProcessorContext

  final protected[this] def error(message: => Any): Unit = {
    if (_logger.isErrorEnabled) {
      _logger.error(s"$taskIdStr$message")
    }
  }

  final protected[this] def info(message: => Any): Unit = {
    if (_logger.isInfoEnabled) {
      _logger.info(s"$taskIdStr$message")
    }
  }

  final protected[this] def warn(message: => Any): Unit = {
    if (_logger.isWarnEnabled) {
      _logger.warn(s"$taskIdStr$message")
    }
  }

  final protected[this] def debug(message: => Any): Unit = {
    if (_logger.isDebugEnabled) {
      _logger.debug(s"$taskIdStr$message")
    }
  }

  final protected[this] def trace(message: => Any): Unit = {
    if (_logger.isTraceEnabled) {
      _logger.trace(s"$taskIdStr$message")
    }
  }

  final protected def timeStr: String = {
    val timestamp = processorContext.timestamp()
    if (timestamp == Long.MaxValue) {
      "@MaxTimestamp"
    } else {
      "@" + new DateTime(processorContext.timestamp())
    }
  }

  final protected def taskIdStr: String = {
    if (processorContext != null && processorContext.taskId != null) {
      processorContext.taskId + "\t"
    } else {
      ""
    }
  }

  implicit class RichLong(long: Long) {
    def iso8601Millis: String = {
      ISODateTimeFormat.dateTime.print(long)
    }

    def iso8601: String = {
      ISODateTimeFormat.dateTimeNoMillis.print(long)
    }
  }

}
