package com.twitter.finatra.kafka.test.utils

import com.twitter.conversions.DurationOps._
import com.twitter.util.Duration
import com.twitter.util.logging.Logging

object PollUtils extends Logging {
  def poll[T](
    func: => T,
    sleepDuration: Duration = 100.millis,
    timeout: Duration = 60.seconds,
    @deprecated("Use timeout", since = "2020-03-02") maxTries: Int = -1,
    pollMessage: String = "",
    exhaustedTimeoutMessage: => String = "",
    exhaustedTriesMessage: (T => String) = (_: T) => ""
  )(
    until: T => Boolean
  ): T = {
    var tries = 0
    var funcResult: T = func

    val timeoutToUse = if (maxTries == -1) {
      timeout
    } else {
      sleepDuration * maxTries
    }

    val endTime = System.currentTimeMillis + timeoutToUse.inMillis

    while (!until(funcResult)) {
      tries += 1

      if (System.currentTimeMillis() > endTime) {
        throw new Exception(
          s"Poll exceeded totalDuration $timeoutToUse: ${exhaustedTriesMessage(funcResult)}"
        )
      }

      if (pollMessage.nonEmpty) {
        info(pollMessage)
      }

      Thread.sleep(sleepDuration.inMillis)
      funcResult = func
    }

    funcResult
  }
}
