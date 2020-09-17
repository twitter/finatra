package com.twitter.inject

import com.google.inject.{Injector => UnderlyingInjector, Provides}
import javax.inject.Singleton

object InjectorModule extends TwitterModule {

  @Provides
  @Singleton
  def providesInjector(injector: UnderlyingInjector): Injector = {
    Injector(injector)
  }
}
