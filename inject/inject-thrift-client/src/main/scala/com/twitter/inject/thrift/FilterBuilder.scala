package com.twitter.inject.thrift

import com.twitter.finagle.stats.StatsReceiver
import com.twitter.inject.Injector
import com.twitter.scrooge.ThriftMethod
import javax.inject.Inject

class FilterBuilder @Inject()(
  injector: Injector,
  statsReceiver: StatsReceiver) {

  def method(method: ThriftMethod): ThriftClientFilterChain[method.Args, method.Result] = {
    new ThriftClientFilterChain[method.Args, method.Result](injector, statsReceiver, method)
  }
}
