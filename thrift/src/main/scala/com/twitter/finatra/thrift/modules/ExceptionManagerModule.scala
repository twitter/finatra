package com.twitter.finatra.thrift.modules

import com.google.inject.Provides
import com.twitter.finagle.stats.StatsReceiver
import com.twitter.finatra.thrift.exceptions.ExceptionManager
import com.twitter.finatra.thrift.internal.exceptions.ThrowableExceptionMapper
import com.twitter.inject.{Injector, TwitterModule}
import javax.inject.Singleton

private[thrift] object ExceptionManagerModule extends TwitterModule {

  @Provides
  @Singleton
  def providesExceptionManager(
    injector: Injector,
    statsReceiver: StatsReceiver
  ): ExceptionManager = {
    new ExceptionManager(injector, statsReceiver)
  }

  /** Add default Framework Exception Mapper */
  override def singletonStartup(injector: Injector): Unit = {
    val manager = injector.instance[ExceptionManager]
    manager.add[ThrowableExceptionMapper]
  }
}
