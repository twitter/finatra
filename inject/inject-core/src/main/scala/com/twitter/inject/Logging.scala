package com.twitter.inject

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.twitter.inject.Logging._
import com.twitter.util.{Future, NonFatal, Stopwatch}
import grizzled.slf4j.{Logger => GrizzledLogger}

object Logging {

  /*
   * Suffix added to the class name of Guice-AOP intercepted classes.
   * When these cglib created classes are detected by name, we'll pass the
   * superclass into the logger to avoid seeing "EnhancerByGuice" in the logs.
   */
  private val GuiceEnhancedSuffix = "$$EnhancerByGuice$$"
}

/**
 * Mix this trait into a class/object to get helpful logging methods.
 *
 * Note: This trait simply adds several methods to the grizzled.slf4j.Logging traits.
 *
 * The methods below are used as so:
 *
 * //Before
 * def foo = {
 * val result = 4 + 5
 * debugResult("Foo returned " + result)
 * result
 * }
 *
 * //After
 * def foo = {
 * debugResult("Foo returned %s") {
 * 4 + 5
 * }
 * }
 */
@JsonIgnoreProperties(Array("trace_enabled", "debug_enabled", "error_enabled", "info_enabled", "warn_enabled"))
trait Logging
  extends grizzled.slf4j.Logging {

  override protected def logger: GrizzledLogger = {
    guiceAwareLogger
  }

  /**
   * Log an error msg that contains the result of the passed in func.
   *
   * @param msg A string containing a single %s which will be replaced with the result of func.
   * @param func The function whose result will be placed in msg.
   * @return Result of func
   */
  protected def errorResult[T](msg: String)(func: => T) = {
    val result = func
    error(msg.format(result))
    result
  }

  /**
   * Log an warn msg that contains the result of the passed in func.
   *
   * @param msg A string containing a single %s which will be replaced with the result of func.
   * @param func The function whose result will be placed in msg.
   * @return Result of func
   */
  protected def warnResult[T](msg: String)(func: => T) = {
    val result = func
    warn(msg.format(result))
    result
  }

  /**
   * Log an info msg that contains the result of the passed in func.
   *
   * @param msg A string containing a single %s which will be replaced with the result of func.
   * @param func The function whose result will be placed in msg.
   * @return Result of func
   */
  protected def infoResult[T](msg: String)(func: => T) = {
    val result = func
    info(msg.format(result))
    result
  }

  /**
   * Log an debug msg that contains the result of the passed in func.
   *
   * @param msg A string containing a single %s which will be replaced with the result of func.
   * @param func The function whose result will be placed in msg.
   * @return Result of func
   */
  protected def debugResult[T](msg: String)(func: => T) = {
    val result = func
    debug(msg.format(result))
    result
  }

  /**
   * Log an debug msg that contains the result of the passed in func.
   *
   * @param msg A string containing a single %s which will be replaced with the result of func.
   * @param func The function returning a future whose result will be placed in msg.
   * @return Result of func
   */
  protected def debugFutureResult[T](msg: String)(func: => Future[T]): Future[T] = {
    func onSuccess { result =>
      debug(msg.format(result))
    } onFailure { e =>
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

  /*
   * If we detect an AOP wrapped class, pass the superclass into the
   * logger to avoid seeing $$EnhancerByGuice$$ in the logs
   */
  private val guiceAwareLogger: GrizzledLogger = {
    if (getClass.getName.contains(GuiceEnhancedSuffix))
      GrizzledLogger(getClass.getSuperclass)
    else
      GrizzledLogger(getClass)
  }
}
