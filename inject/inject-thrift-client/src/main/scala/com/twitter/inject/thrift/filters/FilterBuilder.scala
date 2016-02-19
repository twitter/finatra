package com.twitter.inject.thrift.filters

import com.twitter.finagle.stats.StatsReceiver
import com.twitter.inject.Injector
import com.twitter.inject.thrift.ThriftClientFilterChain
import com.twitter.scrooge.ThriftMethod

class FilterBuilder (
  injector: Injector,
  statsReceiver: StatsReceiver,
  label: String) {

  def method(method: ThriftMethod): ThriftClientFilterChain[method.Args, method.Result] = {
    new ThriftClientFilterChain[method.Args, method.Result](
      injector,
      statsReceiver,
      label,
      method)
  }
}
