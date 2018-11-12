package com.twitter.finatra.thrift.internal.exceptions

import com.twitter.util.Future
import javax.inject.Singleton
import scala.util.control.NonFatal

/**
 * A general [[com.twitter.finatra.thrift.exceptions.ExceptionMapper]] over the Throwable
 * exception type. This ExceptionMapper is the root of our Throwable type hierarchy which throws
 * back any uncaught Throwable `e` as `ConstFuture(Throw(e))`.
 *
 * Users can override this mapper with their own implementation that handles a Throwable type
 */
@Singleton
private[thrift] class ThrowableExceptionMapper
    extends AbstractFrameworkExceptionMapper[Throwable, Throwable] {

  def handle(throwable: Throwable): Future[Throwable] = throwable match {
    case NonFatal(e) => Future.exception(e)
  }
}
