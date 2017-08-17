package com.twitter.finatra.thrift.exceptions

import com.google.inject.Singleton
import com.twitter.finagle.TimeoutException
import com.twitter.finatra.thrift.thriftscala.ClientErrorCause.RequestTimeout
import com.twitter.finatra.thrift.thriftscala.ServerErrorCause.InternalServerError
import com.twitter.finatra.thrift.thriftscala.{
  ClientError,
  NoClientIdError,
  ServerError,
  UnknownClientIdError
}
import com.twitter.inject.Logging
import com.twitter.scrooge.ThriftException
import com.twitter.util.Future
import scala.util.control.NonFatal

/**
 * A generic [[com.twitter.finatra.thrift.exceptions.ExceptionMapper]] over the [[Exception]]
 * exception type. This mapper attempts to translate other exceptions to known finatra-thrift
 * exceptions.
 *
 * We recommend for users to register this mapper in their scala Servers.
 *
 * Note: this is only applicable in scala since it is using thriftscala exceptions
 */
@Singleton
class FinatraThriftExceptionMapper
    extends ExceptionMapper[Exception, ThriftException]
    with Logging {

  def handleException(throwable: Exception): Future[ThriftException] = {
    throwable match {
      case e: TimeoutException =>
        Future.exception(ClientError(RequestTimeout, e.getMessage))
      case e: ClientError =>
        Future.exception(e)
      case e: UnknownClientIdError =>
        Future.exception(e)
      case e: NoClientIdError =>
        Future.exception(e)
      case NonFatal(e) =>
        error("Unhandled exception", e)
        Future.exception(ServerError(InternalServerError, e.getMessage))
    }
  }
}
