package com.twitter.inject.thrift.internal.filters

import com.twitter.finagle.{Service, SimpleFilter}
import com.twitter.inject.exceptions.PossiblyRetryable._
import com.twitter.inject.thrift.ThriftClientException
import com.twitter.scrooge.{ThriftMethod, ThriftResponse, ThriftStruct}
import com.twitter.util._

private[thrift] class ThriftClientExceptionFilter[Req <: ThriftStruct, Rep <: ThriftResponse[_]](
  clientLabel: String,
  method: ThriftMethod)
  extends SimpleFilter[Req, Rep] {

  override def apply(
    request: Req,
    service: Service[Req, Rep]): Future[Rep] = {

    val response = service(request)

    response.transform {
      case Throw(e) if possiblyRetryable(e) =>
        Future.exception(
          ThriftClientException(clientLabel, method, e))
      case Return(result) if result.firstException exists possiblyRetryable =>
        Future.exception(
          ThriftClientException(clientLabel, method, result.firstException().get))
      case _ =>
        response
    }
  }
}
