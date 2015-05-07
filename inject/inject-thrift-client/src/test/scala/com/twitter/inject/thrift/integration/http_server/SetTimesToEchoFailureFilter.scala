package com.twitter.inject.thrift.integration.http_server

import com.twitter.finagle.{Failure, Service}
import com.twitter.inject.thrift.ThriftClientMethodFilter
import com.twitter.inject.thrift.internal.FinatraThriftClientRequest
import com.twitter.util.{Return, Future}

class SetTimesToEchoFailureFilter extends ThriftClientMethodFilter[Int] {

  override def apply(request: FinatraThriftClientRequest, service: Service[FinatraThriftClientRequest, Int]): Future[Int] = {
    service(request) partialTransform {
      case Return(response) if response < 0 =>
        Future.exception(
          Failure("retrying negative response", Failure.Restartable))
    }
  }
}
