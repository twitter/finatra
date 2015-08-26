package com.twitter.finatra.http.integration.startup

import com.twitter.finatra.http.internal.server.BaseHttpServer
import com.twitter.finatra.http.modules.ResponseBuilderModule
import com.twitter.inject.Test
import com.twitter.inject.server.EmbeddedTwitterServer

class BaseHttpServerStartupIntegrationTest extends Test {

  "BaseHttpServer startup" in {
    new EmbeddedTwitterServer(
      twitterServer = new BaseHttpServer {
        override val modules = Seq(ResponseBuilderModule)
      }).start()
  }
}
