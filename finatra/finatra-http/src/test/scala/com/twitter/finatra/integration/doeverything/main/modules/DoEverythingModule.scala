package com.twitter.finatra.integration.doeverything.main.modules

import com.google.inject.Provides
import com.google.inject.name.{Names, Named}
import com.twitter.conversions.time._
import com.twitter.finatra.guice.GuiceModule
import com.twitter.finatra.integration.doeverything.main.services.{ComplexServiceFactory, MultiService, OneMultiService, TwoMultiService}
import com.twitter.finatra.test.Prod

object DoEverythingModule extends GuiceModule {

  // Note: The following flag values are not used in this module, but are @Flag injected elsewhere
  flag("moduleMagicNum", "30", "Module Magic number")
  flag("moduleDuration", 5.seconds, "Module duration")

  override protected def configure() {
    bindSingleton[String](Names.named("str1")).toInstance("string1")
    bind[String](Names.named("str2")).toInstance("string2")

    bindSingleton[Int].toInstance(11)
    bind[String].toInstance("default string")
    bind[String].annotatedWith[Prod].toInstance("prod string")

    bindAssistedFactory[ComplexServiceFactory]

    val multiBinder = createMultiBinder[MultiService]
    multiBinder.addBinding.to[OneMultiService]
    multiBinder.addBinding.to[TwoMultiService]

    singletonStartup { injector =>
      assert(injector.instance[String, Prod] == "prod string")
    }

    singletonPostWarmup {
      info("post warmup")
    }

    singletonShutdown {
      info("shutdown")
    }
  }

  @Provides
  @Named("example")
  def provideNamedString: String = {
    "named"
  }
}
