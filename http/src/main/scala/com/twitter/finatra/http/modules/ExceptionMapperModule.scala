package com.twitter.finatra.http.modules

import com.twitter.finatra.http.exceptions.DefaultExceptionMapper
import com.twitter.finatra.http.internal.exceptions._
import com.twitter.finatra.http.internal.exceptions.json.{JsonParseExceptionMapper, JsonObjectParseExceptionMapper}
import com.twitter.inject.{TwitterModule, Injector, InjectorModule}

object ExceptionMapperModule extends TwitterModule {
  override val modules = Seq(InjectorModule)

  override def configure() {
    bindSingleton[DefaultExceptionMapper].to[FinatraDefaultExceptionMapper]
  }

  override def singletonStartup(injector: Injector) {
      val manager = injector.instance[ExceptionManager]
      manager.add[JsonParseExceptionMapper]
      manager.add[JsonObjectParseExceptionMapper]
      manager.add[HttpExceptionMapper]
      manager.add[HttpResponseExceptionMapper]
      manager.add[CancelledRequestExceptionMapper]
  }
}
