package com.twitter.inject.thrift.internal

import com.twitter.finagle.Filter
import com.twitter.finagle.Service
import com.twitter.inject.thrift.AndThenService
import com.twitter.scrooge.ThriftMethod
import com.twitter.scrooge.ThriftStruct

@deprecated("No replacement.", "2022-03-03")
private[thrift] class DefaultAndThenServiceImpl extends AndThenService {
  def andThen[Req <: ThriftStruct, Rep](
    method: ThriftMethod,
    filter: Filter[Req, Rep, Req, Rep],
    service: Service[Req, Rep]
  ): Service[Req, Rep] = {
    filter.andThen(service)
  }
}
