package ch.qos.logback.core

import com.twitter.finagle.stats.StatsReceiver

/**
 * This extension to [[LogbackAsyncAppenderBase]] provides a way to stop
 * the worker thread from taking events off the blocking queue. This can be
 * used to intentionally block an AsyncAppender for testing.
 *
 * @param statsReceiver the [[StatsReceiver]] to use for emitting stats.
 * @param stopAsyncWorkerThread stop the asynchronous worker thread
 */
class TestLogbackAsyncAppender(
  statsReceiver: StatsReceiver,
  stopAsyncWorkerThread: Boolean = false
) extends LogbackAsyncAppenderBase(statsReceiver) {

  override def start(): Unit = {
    super.start()
    if (stopAsyncWorkerThread) {
      worker.interrupt()
      worker.stop()
    }
  }
}
