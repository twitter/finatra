package com.twitter.finatra.http.modules

import com.google.inject.Module
import com.twitter.finatra.http.internal.marshalling.mustache.MustacheMessageBodyWriter
import com.twitter.finatra.http.internal.marshalling.{
  DefaultMessageBodyReaderImpl,
  DefaultMessageBodyWriterImpl,
  MessageBodyManager
}
import com.twitter.finatra.http.marshalling.{DefaultMessageBodyReader, DefaultMessageBodyWriter}
import com.twitter.finatra.http.marshalling.mustache.MustacheBodyComponent
import com.twitter.finatra.http.response.Mustache
import com.twitter.inject.{Injector, InjectorModule, TwitterModule}

/**
 * Provided implementations for the [[com.twitter.finatra.http.marshalling.DefaultMessageBodyReader]]
 * and the [[com.twitter.finatra.http.marshalling.DefaultMessageBodyWriter]].
 *
 * Also binds the [[com.twitter.finatra.http.internal.marshalling.mustache.MustacheMessageBodyWriter]]
 * which provides the mustache rendering support for the framework.
 *
 * @see [[com.twitter.finatra.http.marshalling.DefaultMessageBodyReader]]
 * @see [[com.twitter.finatra.http.marshalling.DefaultMessageBodyWriter]]
 * @see [[com.twitter.finatra.http.internal.marshalling.MessageBodyManager]]
 */
object MessageBodyModule extends TwitterModule {

  flag("http.response.charset.enabled", true, "Return HTTP Response Content-Type UTF-8 Charset")

  override val modules: Seq[Module] = Seq(InjectorModule)

  override def configure(): Unit = {
    bindSingleton[DefaultMessageBodyReader].to[DefaultMessageBodyReaderImpl]
    bindSingleton[DefaultMessageBodyWriter].to[DefaultMessageBodyWriterImpl]
  }

  override def singletonStartup(injector: Injector): Unit = {
    debug("Configuring MessageBodyManager")
    val manager = injector.instance[MessageBodyManager]
    manager.addByAnnotation[Mustache, MustacheMessageBodyWriter]()
    manager.addByComponentType[MustacheBodyComponent, MustacheMessageBodyWriter]()
  }
}
