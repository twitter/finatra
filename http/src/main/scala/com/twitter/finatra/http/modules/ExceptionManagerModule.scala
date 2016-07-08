package com.twitter.finatra.http.modules

import com.google.inject.Provides
import com.twitter.finagle.stats.StatsReceiver
import com.twitter.finatra.http.exceptions.{ExceptionManager, DefaultExceptionMapper}
import com.twitter.inject.{Injector, TwitterModule}
import javax.inject.Singleton

object ExceptionManagerModule extends TwitterModule {

  @Provides
  @Singleton
  def providesExceptionManager(
    injector: Injector,
    defaultExceptionMapper: DefaultExceptionMapper,
    statsReceiver: StatsReceiver
  ): ExceptionManager = {
    new ExceptionManager(injector, defaultExceptionMapper, statsReceiver)
  }
}
