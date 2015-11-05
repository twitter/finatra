package com.twitter.finatra.http.modules


//TODO: use this in HttpServer once we're on Guice v4
import com.twitter.finatra.json.modules.FinatraJacksonModule
import com.twitter.inject.TwitterModule
import com.twitter.inject.modules.StatsReceiverModule

object ResponseBuilderModule extends TwitterModule {
  override val modules = Seq(
    FinatraJacksonModule,
    DocRootModule,
    MessageBodyModule,
    MustacheModule,
    StatsReceiverModule)
}
