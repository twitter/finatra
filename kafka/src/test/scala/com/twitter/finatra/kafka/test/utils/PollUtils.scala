package com.twitter.finatra.kafka.test.utils

import com.twitter.inject.Logging
import com.twitter.inject.conversions.time._
import org.joda.time.Duration

object PollUtils extends Logging {
  def poll[T](
    func: => T,
    sleepDuration: Duration = 100.millis,
    timeout: Duration = 60.seconds,
    @deprecated("Use timeout") maxTries: Int = -1,
    pollMessage: String = "",
    exhaustedTimeoutMessage: => String = "",
    exhaustedTriesMessage: (T => String) = (_: T) => ""
  )(until: T => Boolean
  ): T = {
    var tries = 0
    var funcResult: T = func

    val timeoutToUse = if (maxTries == -1) {
      timeout
    } else {
      maxTries * sleepDuration
    }

    val endTime = System.currentTimeMillis + timeoutToUse.millis

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

      Thread.sleep(sleepDuration.millis)
      funcResult = func
    }

    funcResult
  }
}
