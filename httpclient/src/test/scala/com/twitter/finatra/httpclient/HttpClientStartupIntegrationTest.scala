package com.twitter.finatra.httpclient

import com.twitter.finatra.httpclient.modules.HttpClientModule
import com.twitter.finatra.json.modules.FinatraJacksonModule
import com.twitter.inject.WordSpecTest
import com.twitter.inject.app.TestInjector

class HttpClientStartupIntegrationTest extends WordSpecTest {

  "startup non ssl" in {
    val injector = TestInjector(
      modules = Seq(FinatraJacksonModule, new HttpClientModule {
        override val dest = "flag!myservice"
      }),
      flags = Map(
        "com.twitter.server.resolverMap" -> "myservice=nil!"))

    injector.instance[HttpClient]
  }

  "startup ssl" in {
    val injector = TestInjector(
      modules = Seq(FinatraJacksonModule, new HttpClientModule {
        override val dest = "flag!myservice"
        override val sslHostname = Some("foo")
      }),
      flags = Map(
        "com.twitter.server.resolverMap" -> "myservice=nil!"))

    injector.instance[HttpClient]
  }
}
