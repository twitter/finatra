package com.twitter.finatra.http.modules

import com.google.inject.Module
import com.twitter.finatra.jackson.modules.ScalaObjectMapperModule
import com.twitter.finatra.modules.FileResolverModule
import com.twitter.inject.TwitterModule
import com.twitter.inject.modules.StatsReceiverModule

/** A helper to encapsulate all the necessary modules for injecting a [[com.twitter.finatra.http.response.ResponseBuilder]] */
object ResponseBuilderModule extends TwitterModule {
  override val modules: Seq[Module] =
    Seq(ScalaObjectMapperModule, FileResolverModule, MessageBodyModule, StatsReceiverModule)
}
