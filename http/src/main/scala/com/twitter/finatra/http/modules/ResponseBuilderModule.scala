package com.twitter.finatra.http.modules

import com.twitter.finatra.json.modules.FinatraJacksonModule
import com.twitter.inject.TwitterModule

// TODO: use this in HttpServer once we're on Guice v4
object ResponseBuilderModule extends TwitterModule {
  override val modules = Seq(
    FinatraJacksonModule,
    LocalDocRootFlagModule,
    new MessageBodyModule,
    MustacheModule)
}
