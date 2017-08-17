package com.twitter.finatra.httpclient

import com.twitter.finatra.httpclient.modules.HttpClientModule
import com.twitter.finatra.json.modules.FinatraJacksonModule
import com.twitter.inject.Test
import com.twitter.inject.app.TestInjector

class HttpClientStartupIntegrationTest extends Test {

  test("startup non ssl") {
    val injector = TestInjector(modules = Seq(FinatraJacksonModule, new HttpClientModule {
      override val dest = "flag!myservice"
    }), flags = Map("com.twitter.server.resolverMap" -> "myservice=nil!")).create

    injector.instance[HttpClient]
  }

  test("startup ssl") {
    val injector = TestInjector(
      modules = Seq(FinatraJacksonModule, new HttpClientModule {
        override val dest = "flag!myservice"
        override val sslHostname = Some("foo")
      }),
      flags = Map("com.twitter.server.resolverMap" -> "myservice=nil!")
    ).create

    injector.instance[HttpClient]
  }
}
