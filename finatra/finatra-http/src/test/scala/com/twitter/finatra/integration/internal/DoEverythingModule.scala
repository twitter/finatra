package com.twitter.finatra.integration.internal

import com.google.inject.Provides
import com.google.inject.name.Named
import com.twitter.conversions.time._
import com.twitter.finatra.guice.{GuiceModule, GuicePrivateModule}

object DoEverythingModule extends GuiceModule {

  flag("moduleMagicNum", "30", "Module Magic number")
  flag("moduleDuration", 5.seconds, "Module duration")

  override val modules = Seq(
    new GuicePrivateModule {
      def configure() {
        //TODO
        //bindAndExpose...
        //bindToAnnotation...
      }
    })

  override protected def configure() {
    bindSingleton[String]("str1").toInstance("string1")

    bindAssistedFactory[ComplexServiceFactory]

    val multiBinder = createMultiBinder[MultiService]
    multiBinder.addBinding.to[OneMultiService]
    multiBinder.addBinding.to[TwoMultiService]
  }

  @Provides
  @Named("example")
  def provideNamedString: String = {
    "named"
  }
}
