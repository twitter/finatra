package com.twitter.inject.thrift.internal.filters

import com.twitter.finagle.{CancelledRequestException, Service, SimpleFilter}
import com.twitter.inject.exceptions.NotRetryableException
import com.twitter.inject.thrift.ThriftClientException
import com.twitter.scrooge.{ThriftMethod, ThriftResponse, ThriftStruct}
import com.twitter.util._

class ThriftClientExceptionFilter[Req <: ThriftStruct, Rep <: ThriftResponse[_]](
  method: ThriftMethod)
  extends SimpleFilter[Req, Rep] {

  override def apply(
    request: Req,
    service: Service[Req, Rep]): Future[Rep] = {

    val response = service(request)

    response.transform {
      case Throw(e) if exceptionsToWrap(e) =>
        Future.exception(
          ThriftClientException(method, e))
      case Return(result) if result.firstException exists exceptionsToWrap =>
        Future.exception(
          ThriftClientException(method, result.firstException().get))
      case _ =>
        response
    }
  }

  private def exceptionsToWrap(e: Throwable): Boolean = {
    NonFatal.apply(e) &&
      !e.isInstanceOf[NotRetryableException] &&
      !e.isInstanceOf[CancelledRequestException] &&
      (e.getCause == null || exceptionsToWrap(e.getCause))
  }
}
