package com.twitter.inject.thrift.internal.filters

import com.twitter.finagle.stats.Counter
import com.twitter.finagle.{Service, SimpleFilter}
import com.twitter.util._

private[thrift] class IncrementCounterFilter[Req, Rep](
  counter: Counter)
  extends SimpleFilter[Req, Rep] {

  override def apply(request: Req, service: Service[Req, Rep]): Future[Rep] = {
    counter.incr()
    service(request)
  }
}
