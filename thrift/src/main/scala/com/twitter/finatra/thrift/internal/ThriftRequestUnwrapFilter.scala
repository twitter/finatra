package com.twitter.finatra.thrift.internal

import com.twitter.finagle.{Filter, Service}
import com.twitter.finatra.thrift.ThriftRequest
import com.twitter.util.Future

class ThriftRequestUnwrapFilter[T, U]
  extends Filter[ThriftRequest, U, T, U] {

  override def apply(request: ThriftRequest, service: Service[T, U]): Future[U] = {
    service(
      request.args.asInstanceOf[T])
  }
}