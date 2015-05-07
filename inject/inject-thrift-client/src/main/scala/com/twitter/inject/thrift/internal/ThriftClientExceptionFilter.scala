package com.twitter.inject.thrift.internal

import com.twitter.finagle.{Service, SimpleFilter}
import com.twitter.inject.thrift.ThriftClientException
import com.twitter.util.{NonFatal, Future}

class ThriftClientExceptionFilter
  extends SimpleFilter[FinatraThriftClientRequest, Any] {

  override def apply(request: FinatraThriftClientRequest, service: Service[FinatraThriftClientRequest, Any]): Future[Any] = {
    service(request).rescue {
      case NonFatal(e) =>
        Future.exception(
          ThriftClientException(
            serviceName = request.label,
            methodName = request.methodName,
            exception = e))
    }
  }
}