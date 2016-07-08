package com.twitter.inject.thrift.filters

import com.twitter.finagle.service.Retries.Budget
import com.twitter.finagle.stats.StatsReceiver
import com.twitter.inject.Injector
import com.twitter.inject.thrift.{AndThenService, ThriftClientFilterChain}
import com.twitter.scrooge.ThriftMethod

class ThriftClientFilterBuilder(
  timeoutMultiplier: Int,
  retryMultiplier: Int,
  injector: Injector,
  statsReceiver: StatsReceiver,
  label: String,
  budget: Budget,
  useHighResTimerForRetries: Boolean,
  andThenService: AndThenService) {

  def method(method: ThriftMethod): ThriftClientFilterChain[method.Args, method.Result] = {
    new ThriftClientFilterChain[method.Args, method.Result](
      injector,
      statsReceiver,
      label,
      budget,
      method,
      timeoutMultiplier = timeoutMultiplier,
      retryMultiplier = retryMultiplier,
      useHighResTimerForRetries = useHighResTimerForRetries,
      andThenService)
  }
}
