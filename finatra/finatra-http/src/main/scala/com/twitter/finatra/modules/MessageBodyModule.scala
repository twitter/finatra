package com.twitter.finatra.modules

import com.twitter.finatra.annotations.Mustache
import com.twitter.finatra.guice.{FinatraInjector, GuiceModule}
import com.twitter.finatra.marshalling._
import javax.inject.Inject

object MessageBodyModule extends GuiceModule {

  override val modules = Seq(FinatraInjectorModule)

  override def configure() {
    bind[MessageBodyManagerConfig].asEagerSingleton()
    bindSingleton[DefaultMessageBodyReader].to[JsonMessageBodyReader]
    bindSingleton[DefaultMessageBodyWriter].to[FinatraDefaultMessageBodyWriter]
  }

  class MessageBodyManagerConfig @Inject()(
    manager: MessageBodyManager) {

    manager.addByAnnotation[MustacheMessageBodyWriter](classOf[Mustache])
  }

}
