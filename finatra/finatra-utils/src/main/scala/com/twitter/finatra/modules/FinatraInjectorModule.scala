package com.twitter.finatra.modules

import com.google.inject.{Injector, Provides}
import com.twitter.finatra.guice.{FinatraInjector, GuiceModule}
import javax.inject.Singleton

object FinatraInjectorModule extends GuiceModule {

  @Provides
  @Singleton
  def providesFinatraInjector(injector: Injector): FinatraInjector = {
    FinatraInjector(injector)
  }
}
