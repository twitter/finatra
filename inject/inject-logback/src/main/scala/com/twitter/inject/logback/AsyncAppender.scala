package com.twitter.inject.logback

import ch.qos.logback.core.LogbackAsyncAppenderBase
import com.twitter.finagle.stats.{LoadedStatsReceiver, StatsReceiver}

/**
 * This is an `ch.qos.logback.core.AsyncAppender` that provides debug metrics about the
 * underlying queue and discarded log events via the [[LoadedStatsReceiver]]
 */
class AsyncAppender(statsReceiver: StatsReceiver) extends LogbackAsyncAppenderBase(statsReceiver) {
  def this() = this(LoadedStatsReceiver)
}
