package com.twitter.finatra.http.marshalling.modules

import com.google.inject.{Module, Provides}
import com.twitter.inject.{Injector, TwitterModule}
import com.twitter.inject.annotations.Flag
import com.twitter.finatra.http.marshalling.{
  DefaultMessageBodyReader,
  DefaultMessageBodyReaderImpl,
  DefaultMessageBodyWriter,
  DefaultMessageBodyWriterImpl,
  MessageBodyFlags,
  MessageInjectableTypes
}
import com.twitter.finatra.jackson.ScalaObjectMapper
import com.twitter.finatra.jackson.caseclass.InjectableTypes
import com.twitter.finatra.modules.FileResolverModule
import com.twitter.finatra.utils.FileResolver
import javax.inject.Singleton

/**
 * A [[TwitterModule]] that provides default implementations for [[com.twitter.finatra.http.marshalling.DefaultMessageBodyReader]],
 * and [[com.twitter.finatra.http.marshalling.DefaultMessageBodyWriter]].
 */
object MessageBodyManagerModule extends TwitterModule {
  // java-friendly access to singleton
  def get(): this.type = this

  override val modules: Seq[Module] = Seq(FileResolverModule, MessageBodyFlagsModule)

  override def configure(): Unit = {
    // override the default binding of `InjectableTypes` to the more specific `RequestInjectableTypes`
    bindOption[InjectableTypes].setBinding.toInstance(MessageInjectableTypes)
  }

  @Provides
  @Singleton
  private def providesDefaultMessageBodyReader(
    injector: Injector,
    objectMapper: ScalaObjectMapper
  ): DefaultMessageBodyReader = {
    new DefaultMessageBodyReaderImpl(injector.underlying, objectMapper)
  }

  @Provides
  @Singleton
  private def providesDefaultMessageBodyWriter(
    injector: Injector,
    @Flag(MessageBodyFlags.ResponseCharsetEnabled) includeContentTypeCharset: Boolean,
    fileResolver: FileResolver,
    objectMapper: ScalaObjectMapper
  ): DefaultMessageBodyWriter = {
    new DefaultMessageBodyWriterImpl(includeContentTypeCharset, fileResolver, objectMapper)
  }
}
