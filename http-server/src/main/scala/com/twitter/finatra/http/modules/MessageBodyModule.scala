package com.twitter.finatra.http.modules

import com.google.inject.{Module, Provides}
import com.twitter.finatra.http.marshalling.modules.MessageBodyManagerModule
import com.twitter.finatra.http.marshalling.{
  DefaultMessageBodyReader,
  DefaultMessageBodyWriter,
  MessageBodyManager
}
import com.twitter.inject.{Injector, TwitterModule}
import javax.inject.Singleton

object MessageBodyModule extends MessageBodyModule {
  // java-friendly access to singleton
  def get(): this.type = this
}

/**
 * A [[TwitterModule]] that provides default implementations for [[com.twitter.finatra.http.marshalling.DefaultMessageBodyReader]],
 * and [[com.twitter.finatra.http.marshalling.DefaultMessageBodyWriter]].
 *
 * Extend this module to override the defaults of the bound [[com.twitter.finatra.http.marshalling.MessageBodyManager]].
 *
 * Example:
 *
 * {{{
 *    import com.twitter.finatra.http.marshalling.MessageBodyManager
 *    import com.twitter.finatra.http.modules.MessageBodyModule
 *    import com.twitter.inject.Injector
 *
 *    object CustomizedMessageBodyModule extends MessageBodyModule {
 *      override def configureMessageBodyManager(injector: Injector, builder: MessageBodyManager.Builder): MessageBodyManager.Builder =
 *        builder
 *          .withDefaultMessageBodyReader(MyDefaultReader)
 *          .withDefaultMessageBodyWriter(MyDefaultWriter)
 *    }
 * }}}
 */
class MessageBodyModule extends TwitterModule {

  /**
   * The [[com.twitter.finatra.http.marshalling.modules.MessageBodyManagerModule]] provides the
   * default reader and writer implementations.
   */
  override val frameworkModules: Seq[Module] = Seq(MessageBodyManagerModule)

  /**
   * Override this method to build an instance of [[MessageBodyManager]]. Custom [[DefaultMessageBodyWriter]]
   * and [[DefaultMessageBodyReader]] implementations can be set on the builder. The created
   * MessageBodyManager will override the default one that is bound to the object graph.
   *
   * @return a configured [[MessageBodyManager.Builder]] to that creates the [[MessageBodyManager]] instance.
   */
  protected def configureMessageBodyManager(
    injector: Injector,
    builder: MessageBodyManager.Builder
  ): MessageBodyManager.Builder = builder

  @Provides
  @Singleton
  private def provideMessageBodyManager(
    injector: Injector,
    defaultReader: DefaultMessageBodyReader,
    defaultWriter: DefaultMessageBodyWriter
  ): MessageBodyManager = {
    val builder = MessageBodyManager.builder(injector, defaultReader, defaultWriter)
    configureMessageBodyManager(injector, builder).build()
  }
}
