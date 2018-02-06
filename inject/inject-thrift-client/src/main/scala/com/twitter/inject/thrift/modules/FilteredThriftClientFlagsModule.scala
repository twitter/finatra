package com.twitter.inject.thrift.modules

import com.twitter.inject.TwitterModule

@deprecated("No replacement. Timeouts can be manipulated with Tunables", "2018-01-08")
object FilteredThriftClientFlagsModule extends TwitterModule {
  flag(
    "timeout.multiplier",
    1,
    "Timeout multiplier to increase specified timeout durations by a common factor"
  )
  flag(
    "retry.multiplier",
    1,
    "Retry multiplier to increase specified retry durations by a common factor"
  )
}
