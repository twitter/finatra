package com.twitter.inject

import com.google.inject.{Injector => GuiceInjector, Provides}
import javax.inject.Singleton

object InjectorModule extends TwitterModule {

  @Provides
  @Singleton
  def providesInjector(injector: GuiceInjector): Injector = {
    Injector(injector)
  }
}
