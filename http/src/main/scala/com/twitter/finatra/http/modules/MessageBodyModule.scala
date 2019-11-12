package com.twitter.finatra.http.modules

import com.google.inject.Module
import com.twitter.finatra.http.internal.marshalling.{
  DefaultMessageBodyReaderImpl,
  DefaultMessageBodyWriterImpl
}
import com.twitter.finatra.http.marshalling.{DefaultMessageBodyReader, DefaultMessageBodyWriter}
import com.twitter.inject.{InjectorModule, TwitterModule}

/**
 * Provided implementations for the [[com.twitter.finatra.http.marshalling.DefaultMessageBodyReader]]
 * and the [[com.twitter.finatra.http.marshalling.DefaultMessageBodyWriter]].
 *
 * @see [[com.twitter.finatra.http.marshalling.DefaultMessageBodyReader]]
 * @see [[com.twitter.finatra.http.marshalling.DefaultMessageBodyWriter]]
 * @see [[com.twitter.finatra.http.marshalling.MessageBodyManager]]
 */
object MessageBodyModule extends MessageBodyModule

class MessageBodyModule extends TwitterModule {

  override val modules: Seq[Module] = Seq(InjectorModule, MessageBodyFlagsModule)

  override def configure(): Unit = {
    bindSingleton[DefaultMessageBodyReader].to[DefaultMessageBodyReaderImpl]
    bindSingleton[DefaultMessageBodyWriter].to[DefaultMessageBodyWriterImpl]
  }
}
