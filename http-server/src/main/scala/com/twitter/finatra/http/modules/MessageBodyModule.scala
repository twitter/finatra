package com.twitter.finatra.http.modules

import com.google.inject.Module
import com.twitter.finatra.http.marshalling.MessageInjectableTypes
import com.twitter.finatra.http.marshalling.modules.MessageBodyManagerModule
import com.twitter.finatra.jackson.caseclass.InjectableTypes
import com.twitter.inject.TwitterModule

/**
 * A [[TwitterModule]] that provides default implementations for [[com.twitter.finatra.http.marshalling.DefaultMessageBodyReader]],
 * and [[com.twitter.finatra.http.marshalling.DefaultMessageBodyWriter]] and assigns a default binding for
 * [[com.twitter.finatra.jackson.caseclass.InjectableTypes]] to [[com.twitter.finatra.http.marshalling.MessageInjectableTypes]].
 */
object MessageBodyModule extends TwitterModule {
  // java-friendly access to singleton
  def get(): this.type = this

  /**
   * The [[com.twitter.finatra.http.marshalling.MessageBodyManagerModule]] provide the default reader
   * and writer implementations
   */
  override val modules: Seq[Module] = Seq(MessageBodyManagerModule)

  override def configure(): Unit = {
    // override the default binding of `InjectableTypes` to the more specific `RequestInjectableTypes`
    bindOption[InjectableTypes].setBinding.toInstance(MessageInjectableTypes)
  }
}
