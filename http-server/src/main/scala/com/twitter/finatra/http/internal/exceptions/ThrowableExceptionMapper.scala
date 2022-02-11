package com.twitter.finatra.http.internal.exceptions

import com.twitter.finagle.http.Request
import com.twitter.finagle.http.Response
import com.twitter.finatra.http.internal.exceptions.ThrowableExceptionMapper._
import com.twitter.finatra.http.response.ResponseBuilder
import com.twitter.inject.utils.ExceptionUtils._
import com.twitter.util.logging.Logger
import javax.inject.Inject
import javax.inject.Singleton

private[exceptions] object ThrowableExceptionMapper {
  val DefaultExceptionSource = "Internal"

  val logger: Logger = Logger(ThrowableExceptionMapper.getClass)

  def unhandledExceptionResponse(
    request: Request,
    response: ResponseBuilder,
    throwable: Throwable
  ): Response = {

    response.internalServerError
      .failure(
        request,
        source = DefaultExceptionSource,
        details = Seq("Unhandled", toExceptionDetails(throwable))
      )
      .jsonError
  }
}

/**
 * A general [[com.twitter.finatra.http.exceptions.ExceptionMapper]] over the Throwable
 * exception type. This mapper specifically attempts to handle the following exceptions
 * (with a default handling of any other exception type):
 *
 * [[com.twitter.finatra.http.exceptions.HttpException]]
 * [[com.twitter.finatra.http.exceptions.HttpResponseException]]
 * [[com.twitter.finagle.CancelledRequestException]]
 * [[org.apache.thrift.TException]]
 *
 * Each exception is handled by a `xxxExceptionResponse` method, with the default
 * behavior for any "unhandled" type implemented in the unhandledExceptionResponse method.
 *
 * Users can subclass this mapper and provide their own implementation of any
 * of the `xxxExceptionResponse` methods to customize how that exception type is
 * converted into a [[com.twitter.finagle.http.Response]]. That subclass can then be
 * registered over the Throwable exception type.
 *
 * @see [[https://twitter.github.io/finatra/user-guide/build-new-http-server/exceptions.html#override-defaults]]
 * @param response - a [[com.twitter.finatra.http.response.ResponseBuilder]]
 */
@Singleton
private[http] class ThrowableExceptionMapper @Inject() (response: ResponseBuilder)
    extends AbstractFrameworkExceptionMapper[Throwable](response) {

  override protected def handle(
    request: Request,
    response: ResponseBuilder,
    throwable: Throwable
  ): Response = {

    logger.error("Unhandled Exception", throwable)
    unhandledExceptionResponse(request, response, throwable)
  }
}
