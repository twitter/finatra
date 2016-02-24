package com.twitter.finatra.http.modules

import com.twitter.finatra.http.internal.marshalling.mustache.MustacheMessageBodyWriter
import com.twitter.finatra.http.internal.marshalling.{FinatraDefaultMessageBodyReader, FinatraDefaultMessageBodyWriter, MessageBodyManager}
import com.twitter.finatra.http.marshalling._
import com.twitter.finatra.http.marshalling.mustache.MustacheBodyComponent
import com.twitter.finatra.response.Mustache
import com.twitter.inject.{Injector, InjectorModule, TwitterModule}

object MessageBodyModule extends TwitterModule {

  override val modules = Seq(InjectorModule)

  override def configure() {
    bindSingleton[DefaultMessageBodyReader].to[FinatraDefaultMessageBodyReader]
    bindSingleton[DefaultMessageBodyWriter].to[FinatraDefaultMessageBodyWriter]
  }

  override def singletonStartup(injector: Injector) {
    debug("Configuring MessageBodyManager")
    val manager = injector.instance[MessageBodyManager]
    manager.addByAnnotation[Mustache, MustacheMessageBodyWriter]
    manager.addByComponentType[MustacheBodyComponent, MustacheMessageBodyWriter]
  }
}
