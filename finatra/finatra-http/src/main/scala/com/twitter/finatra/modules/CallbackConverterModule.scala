package com.twitter.finatra.modules

import com.google.inject.Provides
import com.twitter.finatra.bindings.CallbackConverterPool
import com.twitter.finatra.guice.GuiceModule
import com.twitter.finatra.utils.FuturePools
import com.twitter.util.FuturePool
import javax.inject.Singleton

object CallbackConverterModule extends GuiceModule {

  @Provides
  @Singleton
  @CallbackConverterPool
  def providesPool: FuturePool = {
    FuturePools.unboundedPool("CallbackConverter")
  }
}
