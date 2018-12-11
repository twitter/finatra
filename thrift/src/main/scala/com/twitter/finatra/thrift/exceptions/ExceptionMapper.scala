package com.twitter.finatra.thrift.exceptions

import com.twitter.util.Future

/**
 * An ExceptionMapper converts a `T`-typed throwable to a Future[Rep].
 */
abstract class ExceptionMapper[T <: Throwable, Rep] {

  /**
   * Handles an exception of type [[T]] and maps it to a new Future
   * @param throwable - the Exception [[T]] to handle
   * @return a new Future mapped from the Throwable
   */
  def handleException(throwable: T): Future[Rep]
}
