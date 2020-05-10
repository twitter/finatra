package com.twitter.finatra.thrift.filters

import com.twitter.finagle.{Filter, Service}
import com.twitter.finatra.thrift.exceptions.ExceptionManager
import com.twitter.util.Future
import javax.inject.{Inject, Singleton}

/**
 * A [[ThriftFilter]] which handles exceptions by rescuing the exception and passing
 * it to the [[ExceptionManager]] to handle it.
 *
 * @note This Filter SHOULD be as close to the start of the Filter chain as possible
 */
@Singleton
class ExceptionMappingFilter @Inject() (
  exceptionManager: ExceptionManager)
    extends Filter.TypeAgnostic {

  def toFilter[T, U]: Filter[T, U, T, U] = new Filter[T, U, T, U] {
    def apply(
      request: T,
      service: Service[T, U]
    ): Future[U] = {
      service(request).rescue {
        case e => exceptionManager.handleException(e)
      }
    }
  }
}
