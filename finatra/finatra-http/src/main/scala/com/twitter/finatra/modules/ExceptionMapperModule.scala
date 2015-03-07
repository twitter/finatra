package com.twitter.finatra.modules

import com.twitter.finatra.exceptions.DefaultExceptionMapper
import com.twitter.finatra.internal.exceptions.{ExceptionManager, FinatraDefaultExceptionMapper}
import com.twitter.finatra.internal.exceptions.json.{JsonParseExceptionMapper, JsonObjectParseExceptionMapper}
import com.twitter.inject.{TwitterModule, InjectorModule}

class ExceptionMapperModule extends TwitterModule {
  override val modules = Seq(InjectorModule)

  override def configure() {
    bindSingleton[DefaultExceptionMapper].to[FinatraDefaultExceptionMapper]

    singletonStartup { injector =>
      val manager = injector.instance[ExceptionManager]
      manager.add[JsonParseExceptionMapper]
      manager.add[JsonObjectParseExceptionMapper]
    }
  }
}
