package com.twitter.finatra.modules

import com.google.inject.Provides
import com.twitter.finatra.annotations.CallbackConverterPool
import com.twitter.finatra.guice.GuiceModule
import com.twitter.finatra.utils.FuturePoolUtils
import com.twitter.util.FuturePool

object CallbackConverterModule extends GuiceModule {

  @Provides
  @CallbackConverterPool
  def providesPool: FuturePool = {
    FuturePoolUtils.unboundedPool("CallbackConverter")
  }
}
