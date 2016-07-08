package com.twitter.inject.thrift.internal

import com.twitter.finagle.{Filter, Service}
import com.twitter.inject.thrift.AndThenService
import com.twitter.scrooge.{ThriftMethod, ThriftResponse, ThriftStruct}

private[thrift] class DefaultAndThenServiceImpl extends AndThenService {

  override def andThen[Req <: ThriftStruct, Rep <: ThriftResponse[_]](
    method: ThriftMethod,
    filter: Filter[Req, Rep, Req, Rep],
    service: Service[Req, Rep]
  ): Service[Req, Rep] = {
    filter andThen service
  }
}
