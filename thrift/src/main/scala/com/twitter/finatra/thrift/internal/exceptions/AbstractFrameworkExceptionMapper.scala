package com.twitter.finatra.thrift.internal.exceptions

import com.twitter.finatra.thrift.exceptions.ExceptionMapper
import com.twitter.util.Future

/**
 * Framework Exception mapper aims to handle all users' uncaught exceptions
 *
 * @tparam T the Exception[T] to handle
 * @tparam Rep the response mapped from T
 */
private[exceptions] abstract class AbstractFrameworkExceptionMapper[T <: Throwable, Rep]
    extends ExceptionMapper[T, Rep] {

  final override def handleException(throwable: T): Future[Rep] = {
    handle(throwable)
  }

  protected def handle(throwable: T): Future[Rep]
}
