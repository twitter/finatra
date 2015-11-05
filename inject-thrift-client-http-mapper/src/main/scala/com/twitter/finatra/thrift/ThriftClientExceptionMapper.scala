package com.twitter.finatra.thrift

import com.twitter.finagle.http.{Request, Response, Status}
import com.twitter.finatra.http.exceptions.ExceptionMapper
import com.twitter.finatra.http.response.{ErrorsResponse, ResponseBuilder}
import com.twitter.inject.thrift.ThriftClientException
import com.twitter.util.Throwables
import javax.inject.{Inject, Singleton}

@Singleton
class ThriftClientExceptionMapper @Inject()(
  response: ResponseBuilder)
  extends ExceptionMapper[ThriftClientException] {

  override def toResponse(
    request: Request,
    exception: ThriftClientException): Response = {

    response
      .status(Status.ServiceUnavailable)
      .handled(
        request,
        exception,
        exception.method.serviceName,
        exception.method.name,
        Throwables.mkString(exception.cause).mkString("/"))
      .jsonError
  }
}