package com.twitter.finatra.http.modules

import com.twitter.finatra.http.exceptions.DefaultExceptionMapper
import com.twitter.finatra.http.internal.exceptions.{ExceptionManager, FinatraDefaultExceptionMapper}
import com.twitter.finatra.http.internal.exceptions.json.{JsonParseExceptionMapper, CaseClassExceptionMapper}
import com.twitter.inject.{TwitterModule, Injector, InjectorModule}

object ExceptionMapperModule extends TwitterModule {
  override val modules = Seq(InjectorModule)

  override def configure() {
    bindSingleton[DefaultExceptionMapper].to[FinatraDefaultExceptionMapper]
  }

  override def singletonStartup(injector: Injector) {
    val manager = injector.instance[ExceptionManager]
    manager.add[JsonParseExceptionMapper]
    manager.add[CaseClassExceptionMapper]
  }
}
