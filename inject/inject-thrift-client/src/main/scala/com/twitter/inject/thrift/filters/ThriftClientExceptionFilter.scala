package com.twitter.inject.thrift.filters

import com.twitter.finagle.{Service, SimpleFilter}
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
      case Throw(NonFatal(e)) =>
        Future.exception(
          ThriftClientException(method, e))
      case Return(result) if result.firstException exists NonFatal.isNonFatal =>
        Future.exception(
          ThriftClientException(method, result.firstException().get))
      case _ =>
        response
    }
  }
}
