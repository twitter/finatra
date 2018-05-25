package ch.qos.logback.core

import com.twitter.finagle.stats.StatsReceiver

/**
 * This extension to [[LogbackAsyncAppenderBase]] provides a way to stop
 * the worker thread from taking events off the blocking queue. This can be
 * used to intentionally block an AsyncAppender for testing.
 *
 * @param statsReceiver
 * @param stopWorker stop worker thread
 */
class TestLogbackAsyncAppenderBase(statsReceiver: StatsReceiver, stopWorker: Boolean = false)
    extends LogbackAsyncAppenderBase(statsReceiver) {
  override def start(): Unit = {
    super.start()

    if (stopWorker) worker.stop()
  }
}
