package com.twitter.finatra.http.modules

import com.google.inject.Module
import com.twitter.finatra.json.modules.FinatraJacksonModule
import com.twitter.finatra.modules.FileResolverModule
import com.twitter.inject.TwitterModule
import com.twitter.inject.modules.StatsReceiverModule

@deprecated("Define the modules separately", "2019-10-16")
object ResponseBuilderModule extends TwitterModule {
  override val modules: Seq[Module] =
    Seq(FinatraJacksonModule, FileResolverModule, MessageBodyModule, StatsReceiverModule)
}
