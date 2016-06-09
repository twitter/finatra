package com.twitter.finatra.thrift

import com.twitter.finagle.http.{Request, Response, Status}
import com.twitter.finatra.http.exceptions.ExceptionMapper
import com.twitter.finatra.http.response.ResponseBuilder
import com.twitter.inject.Logging
import com.twitter.inject.utils.ExceptionUtils._
import com.twitter.inject.thrift.{ThriftClientException, ThriftClientExceptionSource}
import javax.inject.{Inject, Singleton}

@Singleton
class ThriftClientExceptionMapper @Inject()(
  response: ResponseBuilder,
  source: ThriftClientExceptionSource)
  extends ExceptionMapper[ThriftClientException]
  with Logging {

  override def toResponse(
    request: Request,
    exception: ThriftClientException
  ): Response = {
    response
      .status(Status.ServiceUnavailable)
      .jsonError
      .failure(
        request,
        source(exception),
        details = Seq(
          exception.method.serviceName,
          exception.method.name,
          toExceptionDetails(exception.cause)),
        message = toExceptionMessage(exception.cause))
  }
}