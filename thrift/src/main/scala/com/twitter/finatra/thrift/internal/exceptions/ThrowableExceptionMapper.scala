package com.twitter.finatra.thrift.internal.exceptions

import com.google.inject.Singleton
import com.twitter.util.Future
import scala.util.control.NonFatal

/**
 * A general [[com.twitter.finatra.thrift.exceptions.ExceptionMapper]] over the Throwable
 * exception type.
 *
 * Users can override this mapper with their own implementation that handles a Throwable type
 */
@Singleton
private[thrift] class ThrowableExceptionMapper
  extends AbstractFrameworkExceptionMapper[Throwable, Throwable] {

  def handle(throwable: Throwable): Future[Throwable] = {
    throwable match {
      case NonFatal(e) => Future.exception(e)
    }
  }
}
