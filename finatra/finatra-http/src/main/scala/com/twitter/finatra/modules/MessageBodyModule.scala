package com.twitter.finatra.modules

import com.twitter.finatra.guice.GuiceModule
import com.twitter.finatra.marshalling._
import com.twitter.finatra.marshalling.mustache.MustacheMessageBodyWriter
import com.twitter.finatra.response.Mustache

object MessageBodyModule extends MessageBodyModule

class MessageBodyModule extends GuiceModule {

  override val modules = Seq(FinatraInjectorModule)

  override def configure() {
    bindSingleton[DefaultMessageBodyReader].to[FinatraDefaultMessageBodyReader]
    bindSingleton[DefaultMessageBodyWriter].to[FinatraDefaultMessageBodyWriter]

    singletonStartup { injector =>
      val manager = injector.instance[MessageBodyManager]
      manager.addByAnnotation[Mustache, MustacheMessageBodyWriter]
    }
  }
}
