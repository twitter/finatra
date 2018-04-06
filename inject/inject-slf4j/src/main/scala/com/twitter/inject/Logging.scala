package com.twitter.inject

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.twitter.util.{Future, Return, Stopwatch, Throw}
import scala.util.control.NonFatal

/**
 * Mix this trait into a class/object to get helpful logging methods.
 *
 * @note This trait simply adds several methods to the [[com.twitter.util.logging.Logging]] trait.
 *
 * The methods below are used as so:
 *
 * Before:
 * {{{
 *  def foo = {
 *    val result = 4 + 5
 *    debugFutureResult("Foo returned " + result)
 *    result
 *  }
 * }}}
 *
 * After:
 * {{{
 *  def foo = {
 *    debugFutureResult("Foo returned %s") {
 *      4 + 5
 *    }
 *  }
 * }}}
 */
@JsonIgnoreProperties(
  Array(
    "logger_name",
    "trace_enabled",
    "debug_enabled",
    "error_enabled",
    "info_enabled",
    "warn_enabled"
  )
)
trait Logging extends com.twitter.util.logging.Logging {

  /**
   * Log an debug msg that contains the result of the passed in func.
   *
   * @param msg A string containing a single %s which will be replaced with the result of func.
   * @param func The function returning a future whose result will be placed in msg.
   * @return Result of func
   */
  protected def debugFutureResult[T](msg: String)(func: => Future[T]): Future[T] = {
    func.respond {
      case Return(result) =>
        debug(msg.format(result))
      case Throw(e) =>
        debug(msg.format(e))
    }
  }

  protected def time[T](formatStr: String)(func: => T): T = {
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
