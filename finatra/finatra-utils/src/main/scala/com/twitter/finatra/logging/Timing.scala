package com.twitter.finatra.logging

import com.twitter.util.{NonFatal, Stopwatch}
import com.twitter.finatra.utils.Logging

trait Timing extends Logging {
  def time[T](formatStr: String)(func: => T): T = {
    val elapsed = Stopwatch.start()
    try {
      val result = func
      info(formatStr.format(elapsed().inMillis, result))
      result
    } catch {
      case NonFatal(e) =>
        error(formatStr.format(elapsed().inMillis, e))
        throw e
    }
  }
}
