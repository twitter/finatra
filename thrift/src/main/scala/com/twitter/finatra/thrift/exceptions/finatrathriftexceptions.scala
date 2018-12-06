package com.twitter.finatra.thrift.exceptions

import com.twitter.finagle.TimeoutException
import com.twitter.inject.Logging
import com.twitter.util.Future
import javax.inject.Singleton
import scala.util.control.NonFatal

/**
 * A generic [[com.twitter.finatra.thrift.exceptions.ExceptionMapper]] over the
 * [[java.lang.Exception]] exception type intended for usage with generated Scala code.
 * This mapper attempts to translate other exceptions to declared
 * `finatra-thrift/finatra_thrift_exceptions.thrift` exceptions.
 *
 * We recommend for users to include the `finatra-thrift/finatra_thrift_exceptions.thrift` and register
 * this mapper in their servers.
 *
 * If you include the `finatra-thrift/finatra_thrift_exceptions.thrift` file in your
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
 * In your `myservice.thrift`, include the `finatra-thrift/finatra_thrift_exceptions.thrift` thrift IDL:
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
 * @note This is only applicable with generated Scala code since the [[FinatraThriftExceptionMapper]]
 *       uses generated Scala classes for the exception mapping. Users of generated Java code should
 *       use [[FinatraJavaThriftExceptionMapper]].
 * @see `finatra-thrift/finatra_thrift_exceptions.thrift`
 * @see [[https://twitter.github.io/finatra/user-guide/thrift/exceptions.html Finatra Thrift Exception Mapping]]
 * @see [[https://twitter.github.io/finagle/guide/Protocols.html#using-finagle-thrift Using Finagle Thrift]]
 */
@Singleton
final class FinatraThriftExceptionMapper
    extends ExceptionMapper[Exception, Nothing]
    with Logging {

  import com.twitter.finatra.thrift.thriftscala

  def handleException(throwable: Exception): Future[Nothing] = {
    throwable match {
      case e: TimeoutException =>
        Future.exception(
          thriftscala.ClientError(thriftscala.ClientErrorCause.RequestTimeout, e.getMessage())
        )
      case e if isHandledThriftException(e) =>
        Future.exception(e)
      case NonFatal(e) =>
        error("Unhandled exception", e)
        Future.exception(
          thriftscala.ServerError(thriftscala.ServerErrorCause.InternalServerError, e.getMessage)
        )
    }
  }

  private[this] def isHandledThriftException(throwable: Exception): Boolean = throwable match {
    case _: thriftscala.ClientError | _: thriftscala.UnknownClientIdError |
        _: thriftscala.NoClientIdError | _: thriftscala.ServerError =>
      true
    case _ =>
      false
  }
}

/**
 * A generic [[com.twitter.finatra.thrift.exceptions.ExceptionMapper]] over the
 * [[java.lang.Exception]] exception type intended for usage with generated Java code.
 * This mapper attempts to translate other exceptions to declared
 * `finatra-thrift/finatra_thrift_exceptions.thrift` exceptions.
 *
 * We recommend for users to include the `finatra-thrift/finatra_thrift_exceptions.thrift` and register
 * this mapper in their servers.
 *
 * If you include the `finatra-thrift/finatra_thrift_exceptions.thrift` file in your
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
 * In your `myservice.thrift`, include the `finatra-thrift/finatra_thrift_exceptions.thrift` thrift IDL:
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
 * @note This is only applicable with generated Java code since the [[FinatraJavaThriftExceptionMapper]]
 *       uses generated Java classes for the exception mapping. Users of generated Scala code should
 *       use [[FinatraThriftExceptionMapper]].
 * @see `finatra-thrift/finatra_thrift_exceptions.thrift`
 * @see [[https://twitter.github.io/finatra/user-guide/thrift/exceptions.html Finatra Thrift Exception Mapping]]
 * @see [[https://twitter.github.io/finagle/guide/Protocols.html#using-finagle-thrift Using Finagle Thrift]]
 */
@Singleton
final class FinatraJavaThriftExceptionMapper
    extends ExceptionMapper[Exception, Nothing]
    with Logging {

  import com.twitter.finatra.thrift.thriftjava

  def handleException(throwable: Exception): Future[Nothing] = {
    throwable match {
      case e: TimeoutException =>
        Future.exception(new thriftjava.ClientError(
          thriftjava.ClientErrorCause.REQUEST_TIMEOUT,
          e.getMessage())
        )
      case e if isHandledThriftException(e) =>
        Future.exception(e)
      case NonFatal(e) =>
        error("Unhandled exception", e)
        Future.exception(new thriftjava.ServerError(
          thriftjava.ServerErrorCause.INTERNAL_SERVER_ERROR,
          e.getMessage)
        )
    }
  }

  private[this] def isHandledThriftException(throwable: Exception): Boolean = throwable match {
    case _: thriftjava.ClientError | _: thriftjava.UnknownClientIdError |
        _: thriftjava.NoClientIdError | _: thriftjava.ServerError =>
      true
    case _ =>
      false
  }
}
