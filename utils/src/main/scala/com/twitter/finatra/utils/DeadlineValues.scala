package com.twitter.finatra.utils

import com.twitter.finagle.Deadline
import com.twitter.finagle.context.Contexts
import com.twitter.inject.Logging
import org.joda.time.DateTime

object DeadlineValues extends Logging {

  /**
   * Note: Deadline already contains an remaining and expired method, but they use Twitter time
   * to calculate "now", however we'd like to continue using Joda-Time (http://www.joda.org/joda-time/)
   * for consistency in how we mock time in testing.
   */
  def current(): Option[DeadlineValues] = {
    for (deadline <- Contexts.broadcast.get(Deadline)) yield {
      debug("Current Deadline: " + deadline)
      val nowMillis = DateTime.now.getMillis
      DeadlineValues(
        elapsed = nowMillis - deadline.timestamp.inMillis,
        remaining = deadline.deadline.inMillis - nowMillis)
    }
  }
}

case class DeadlineValues(
  elapsed: Long,
  remaining: Long) {
  def expired = remaining <= 0
}
