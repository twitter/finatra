package com.twitter.inject.tests.module

import com.google.inject.name.{Named, Names}
import com.google.inject.spi.TypeConverter
import com.google.inject.{Provides, TypeLiteral}
import com.twitter.conversions.time._
import com.twitter.finatra.tests.Prod
import com.twitter.inject.{Injector, TwitterModule}
import java.util.Properties

object DoEverythingModule extends TwitterModule {

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
    getProvider[Int]

    val multiBinder = createMultiBinder[MultiService]
    multiBinder.addBinding.to[OneMultiService]
    multiBinder.addBinding.to[TwoMultiService]

    addTypeConvertor[ClassToConvert](
      new TypeConverter {
        override def convert(s: String, typeLiteral: TypeLiteral[_]): AnyRef = {
          ClassToConvert(s)
        }
      })


    val properties = new Properties()
    properties.setProperty("name", "Steve")
    Names.bindProperties(binder(), properties)
  }

  override def singletonStartup(injector: Injector) {
    assert(injector.instance[String, Prod] == "prod string")
  }

  override def singletonPostWarmupComplete(injector: Injector) {
    info("module post warmup complete")
  }

  override def singletonShutdown(injector: Injector) {
    info("shutdown")
  }

  @Provides
  @Named("example")
  def provideNamedString: String = {
    "named"
  }
}

case class ClassToConvert(
  name: String)