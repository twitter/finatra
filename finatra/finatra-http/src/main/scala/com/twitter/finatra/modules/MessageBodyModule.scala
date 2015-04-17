package com.twitter.finatra.modules

import com.twitter.finatra.internal.marshalling.mustache.MustacheMessageBodyWriter
import com.twitter.finatra.internal.marshalling.{FinatraDefaultMessageBodyReader, FinatraDefaultMessageBodyWriter, MessageBodyManager}
import com.twitter.finatra.marshalling._
import com.twitter.finatra.response.Mustache
import com.twitter.inject.{InjectorModule, TwitterModule}

class MessageBodyModule extends TwitterModule {

  override val modules = Seq(InjectorModule)

  override def configure() {
    bindSingleton[DefaultMessageBodyReader].to[FinatraDefaultMessageBodyReader]
    bindSingleton[DefaultMessageBodyWriter].to[FinatraDefaultMessageBodyWriter]

    singletonStartup { injector =>
      info("Configuring MessageBodyManager")
      val manager = injector.instance[MessageBodyManager]
      manager.addByAnnotation[Mustache, MustacheMessageBodyWriter]
    }
  }
}
