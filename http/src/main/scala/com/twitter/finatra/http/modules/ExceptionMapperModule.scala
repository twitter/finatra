package com.twitter.finatra.http.modules

import com.twitter.finatra.http.exceptions.{DefaultExceptionMapper, ExceptionManager}
import com.twitter.finatra.http.internal.exceptions.json.{CaseClassExceptionMapper, JsonParseExceptionMapper}
import com.twitter.finatra.http.internal.exceptions.FinatraDefaultExceptionMapper
import com.twitter.inject.{Injector, TwitterModule}

object ExceptionMapperModule extends TwitterModule {
  override def configure() {
    bindSingleton[DefaultExceptionMapper].to[FinatraDefaultExceptionMapper]
  }

  override def singletonStartup(injector: Injector) {
    val manager = injector.instance[ExceptionManager]
    manager.add[JsonParseExceptionMapper]
    manager.add[CaseClassExceptionMapper]
  }
}
