package com.twitter.inject.thrift.filters

import com.twitter.finagle.service.Retries.Budget
import com.twitter.finagle.stats.StatsReceiver
import com.twitter.inject.Injector
import com.twitter.inject.thrift.AndThenService
import com.twitter.scrooge.ThriftMethod

@deprecated("Use ThriftMethodBuilderClientModule and ThriftMethodBuilder", "2018-01-12")
class ThriftClientFilterBuilder(
  timeoutMultiplier: Int,
  retryMultiplier: Int,
  injector: Injector,
  val statsReceiver: StatsReceiver,
  label: String,
  budget: Budget,
  useHighResTimerForRetries: Boolean,
  andThenService: AndThenService
) {

  def method(method: ThriftMethod): ThriftClientFilterChain[method.Args, method.SuccessType] = {
    new ThriftClientFilterChain[method.Args, method.SuccessType](
      injector,
      statsReceiver,
      label,
      budget,
      method,
      timeoutMultiplier = timeoutMultiplier,
      retryMultiplier = retryMultiplier,
      useHighResTimerForRetries = useHighResTimerForRetries,
      andThenService
    )
  }
}
