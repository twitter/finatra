package com.twitter.inject.thrift.internal

import com.twitter.finagle.stats.Counter
import com.twitter.finagle.{Service, SimpleFilter}
import com.twitter.scrooge.{ThriftResponse, ThriftStruct}
import com.twitter.util._

private[thrift] class IncrementCounterFilter[Req <: ThriftStruct, Rep <: ThriftResponse[_]](
  counter: Counter)
  extends SimpleFilter[Req, Rep] {

  override def apply(request: Req, service: Service[Req, Rep]): Future[Rep] = {
    counter.incr()
    service(request)
  }
}
