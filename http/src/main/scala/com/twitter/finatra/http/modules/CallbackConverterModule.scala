package com.twitter.finatra.http.modules

import com.google.inject.Provides
import com.twitter.finatra.bindings.CallbackConverterPool
import com.twitter.finatra.utils.FuturePools
import com.twitter.inject.TwitterModule
import com.twitter.util.FuturePool
import javax.inject.Singleton

object CallbackConverterModule extends TwitterModule {

  @Provides
  @Singleton
  @CallbackConverterPool
  def providesPool: FuturePool = {
    FuturePools.unboundedPool("CallbackConverter")
  }
}
