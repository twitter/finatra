package com.twitter.finatra.http.modules

import com.google.inject.Provides
import com.twitter.finagle.stats.StatsReceiver
import com.twitter.finatra.http.exceptions.ExceptionManager
import com.twitter.finatra.http.internal.exceptions.ThrowableExceptionMapper
import com.twitter.finatra.http.internal.exceptions.json.{CaseClassExceptionMapper, JsonParseExceptionMapper}
import com.twitter.inject.{Injector, TwitterModule}
import javax.inject.Singleton

object ExceptionManagerModule extends TwitterModule {

  @Provides
  @Singleton
  def providesExceptionManager(
    injector: Injector,
    statsReceiver: StatsReceiver
  ): ExceptionManager = {
    new ExceptionManager(injector, statsReceiver)
  }

  /** Add default Framework Exception Mappers */
  override def singletonStartup(injector: Injector) {
    val manager = injector.instance[ExceptionManager]
    manager.add[ThrowableExceptionMapper]
    manager.add[JsonParseExceptionMapper]
    manager.add[CaseClassExceptionMapper]
  }
}
