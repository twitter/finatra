package com.twitter.finatra.http.integration.startup

import com.twitter.finatra.http.internal.server.BaseHttpServer
import com.twitter.finatra.http.modules.ResponseBuilderModule
import com.twitter.finatra.http.test.EmbeddedHttpServer
import com.twitter.inject.Test

class BaseHttpServerStartupIntegrationTest extends Test {

  "BaseHttpServer startup" in {
    new EmbeddedHttpServer(
      twitterServer = new BaseHttpServer {
        override val modules = Seq(ResponseBuilderModule)
      }).start()
  }
}
