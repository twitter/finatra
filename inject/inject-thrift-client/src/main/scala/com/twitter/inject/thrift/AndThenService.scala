package com.twitter.inject.thrift

import com.twitter.finagle.{Filter, Service}
import com.twitter.scrooge.{ThriftMethod, ThriftResponse, ThriftStruct}

trait AndThenService {
  def andThen[Req <: ThriftStruct, Rep <: ThriftResponse[_]](
    method: ThriftMethod,
    filter: Filter[Req, Rep, Req, Rep],
    service: Service[Req, Rep]
  ): Service[Req, Rep]
}