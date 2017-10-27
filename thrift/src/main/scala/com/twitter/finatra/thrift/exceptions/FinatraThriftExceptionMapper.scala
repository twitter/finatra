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
 * A generic [[com.twitter.finatra.thrift.exceptions.ExceptionMapper]] over the
 * [[java.lang.Exception]] exception type. This mapper attempts to translate other
 * exceptions to declared `finatra-thrift/finatra_thrift_exception.thrift` exceptions.
 *
 * If you include the `finatra-thrift/finatra_thrift_exception.thrift` file in your
 * thrift service IDL and declare any of your service methods to throw one or more of the defined
 * thrift exceptions, i.e.,
 *
 * {{{
 *  finatra_thrift_exceptions.ClientError
 *  finatra_thrift_exceptions.NoClientIdError
 *  finatra_thrift_exceptions.ServerError
 *  finatra_thrift_exceptions.UnknownClientIdError
 * }}}
 *
 * using the FinatraThriftExceptionMapper will translate any [[Throwable]] into
 * one of the finatra_thrift_exceptions where appropriate for responding to the
 * client.
 *
 * Exceptions returned from a thrift service which are not declared in the thrift
 * IDL are returned as a generic `TApplicationException`.
 *
 * =Usage=
 *
 * In your `myservice.thrift`, include the `finatra-thrift/finatra_thrift_exception.thrift` thrift IDL:
 *
 * {{{
 *     namespace java com.my_org.myservice.thriftjava
 *     #@namespace scala com.my_org.myservice.thriftscala
 *
 *     include "finatra-thrift/finatra_thrift_exceptions.thrift"
 * }}}
 *
 * Then in your MyService definition, declare methods to throw finatra_thrift_exceptions:
 *
 * {{{
 *      service MyService {
 *
 *        string function1(
 *          1: string msg
 *        ) throws (
 *          1: finatra_thrift_exceptions.ServerError serverError,
 *          2: finatra_thrift_exceptions.UnknownClientIdError unknownClientIdError
 *          3: finatra_thrift_exceptions.NoClientIdError noClientIdError
 *        )
 * }}}
 *
 * Adding this ExceptionMapper into your server will thus translate any of the handled
 * exceptions into a form that will serialize appropriately to the client.
 *
 * Note, you can also mix usage with your own declared exception(s), e.g.,
 *
 * {{{
 *      exception MyServiceException {
 *        1: string message
 *      }
 * }}}
 *
 * Then in your MyService definition,
 *
 * {{{
 *      service MyService {
 *
 *        string function1(
 *          1: string msg
 *        ) throws (
 *          1: finatra_thrift_exceptions.ServerError serverError,
 *          2: finatra_thrift_exceptions.UnknownClientIdError unknownClientIdError
 *          3: finatra_thrift_exceptions.NoClientIdError noClientIdError
 *          4: MyServiceException myServiceException
 *        )
 * }}}
 *
 * You could then register an additional [[ExceptionMapper]] to translate [[Throwable]]s
 * into `MyServiceException` as desired or return the exception directly from implemented
 * methods.
 *
 * @note This is only applicable in scala since the FinatraThriftExceptionMapper uses
 * scala generated classes for the exceptions. We recommend for users including the
 * `finatra-thrift/finatra_thrift_exception.thrift` register this mapper in their
 * scala Servers.
 *
 * @see src/main/thrift/finatra-thrift/finatra_thrift_exception.thrift
 * @see [[https://twitter.github.io/finatra/user-guide/thrift/exceptions.html Finatra Thrift Exception Mapping]]
 * @see [[https://twitter.github.io/finagle/guide/Protocols.html#using-finagle-thrift Using Finagle Thrift]]
 */
@Singleton
class FinatraThriftExceptionMapper
  extends ExceptionMapper[Exception, ThriftException]
  with Logging {

  def handleException(throwable: Exception): Future[ThriftException] = {
    throwable match {
      case e: TimeoutException =>
        Future.exception(ClientError(RequestTimeout, e.getMessage()))
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
