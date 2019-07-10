package com.twitter.finatra.httpclient

import com.google.inject.Provides
import com.twitter.finagle.Http
import com.twitter.finagle.stats.StatsReceiver
import com.twitter.finatra.httpclient.modules.{HttpClientModule, HttpClientModuleTrait}
import com.twitter.finatra.json.FinatraObjectMapper
import com.twitter.finatra.json.modules.FinatraJacksonModule
import com.twitter.inject.{Injector, InjectorModule, Test}
import com.twitter.inject.app.TestInjector
import com.twitter.inject.modules.StatsReceiverModule

class HttpClientStartupIntegrationTest extends Test {

  test("startup non ssl with HttpClientModule") {
    val injector = TestInjector(
      modules = Seq(InjectorModule, StatsReceiverModule, FinatraJacksonModule, new HttpClientModule {
      override val dest = "flag!myservice"
    }), flags = Map("com.twitter.server.resolverMap" -> "myservice=nil!")).create

    injector.instance[HttpClient]
  }

  test("startup ssl with HttpClientModule") {
    val injector = TestInjector(
      modules = Seq(InjectorModule, StatsReceiverModule, FinatraJacksonModule, new HttpClientModule {
        override val dest = "flag!myservice"
        override val sslHostname = Some("foo")
      }),
      flags = Map("com.twitter.server.resolverMap" -> "myservice=nil!")
    ).create

    injector.instance[HttpClient]
  }

  test("startup non ssl with HttpClientModuleTrait") {
    val injector = TestInjector(
      modules = Seq(InjectorModule, StatsReceiverModule, FinatraJacksonModule, new HttpClientModuleTrait {
        override val label: String = "test-non-ssl"
        override val dest: String = "flag!myservice"

        @Provides
        def providesHttpClient(
          injector: Injector,
          statsReceiver: StatsReceiver,
          mapper: FinatraObjectMapper
        ): HttpClient = newHttpClient(injector, statsReceiver, mapper)

      }), flags = Map("com.twitter.server.resolverMap" -> "myservice=nil!")).create

    injector.instance[HttpClient]
  }

  test("startup ssl with HttpClientModuleTrait") {
    val injector = TestInjector(
      modules = Seq(InjectorModule, StatsReceiverModule, FinatraJacksonModule, new HttpClientModuleTrait {
        override val label: String = "test-ssl"
        override val dest: String = "flag!myservice"

        override def configureClient(
          injector: Injector,
          client: Http.Client
        ): Http.Client = client.withTls("foo")

        @Provides
        def providesHttpClient(
          injector: Injector,
          statsReceiver: StatsReceiver,
          mapper: FinatraObjectMapper
        ): HttpClient = newHttpClient(injector, statsReceiver, mapper)

      }),
      flags = Map("com.twitter.server.resolverMap" -> "myservice=nil!")
    ).create

    injector.instance[HttpClient]
  }
}
