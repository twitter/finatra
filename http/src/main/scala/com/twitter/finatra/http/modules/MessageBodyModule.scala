package com.twitter.finatra.http.modules

import com.twitter.finatra.http.internal.marshalling.mustache.MustacheMessageBodyWriter
import com.twitter.finatra.http.internal.marshalling.{FinatraDefaultMessageBodyReader, FinatraDefaultMessageBodyWriter, MessageBodyManager}
import com.twitter.finatra.http.marshalling._
import com.twitter.finatra.response.Mustache
import com.twitter.inject.{InjectorModule, TwitterModule}

class MessageBodyModule extends TwitterModule {

  override val modules = Seq(InjectorModule)

  override def configure() {
    bindSingleton[DefaultMessageBodyReader].to[FinatraDefaultMessageBodyReader]
    bindSingleton[DefaultMessageBodyWriter].to[FinatraDefaultMessageBodyWriter]

    singletonStartup { injector =>
      debug("Configuring MessageBodyManager")
      val manager = injector.instance[MessageBodyManager]
      manager.addByAnnotation[Mustache, MustacheMessageBodyWriter]
    }
  }
}
