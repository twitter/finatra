package com.twitter.inject

import com.twitter.util.Future
import com.twitter.util.Return
import com.twitter.util.Stopwatch
import com.twitter.util.Throw
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
@deprecated("Use c.t.util.logging.Logging directly", "2022-01-27")
trait Logging extends com.twitter.util.logging.Logging {

  /**
   * Log an debug msg that contains the result of the passed in func.
   *
   * @param msg A string containing a single %s which will be replaced with the result of func.
   * @param func The function returning a future whose result will be placed in msg.
   * @return Result of func
   */
  @deprecated("No replacement", "2022-01-27")
  protected def debugFutureResult[T](msg: String)(func: => Future[T]): Future[T] = {
    func.respond {
      case Return(result) =>
        debug(msg.format(result))
      case Throw(e) =>
        debug(msg.format(e))
    }
  }

  @deprecated("No replacement", "2022-01-27")
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
