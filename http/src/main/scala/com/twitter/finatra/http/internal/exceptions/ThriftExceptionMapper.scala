package com.twitter.finatra.http.internal.exceptions

import com.twitter.finagle.http.{Request, Response}
import com.twitter.finatra.http.internal.exceptions.ThrowableExceptionMapper._
import com.twitter.finatra.http.response.ResponseBuilder
import javax.inject.{Inject, Singleton}
import org.apache.thrift.TException

@Singleton
private[http] class ThriftExceptionMapper @Inject()(
  response: ResponseBuilder)
  extends AbstractExceptionMapper[TException](response) {

  override protected def handle(
    request: Request,
    response: ResponseBuilder,
    exception: TException): Response = {
    unhandledExceptionResponse(request, response, exception)
  }
}