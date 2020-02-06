package com.twitter.finatra.httpclient

import com.google.inject.Provides
import com.twitter.finagle.Http
import com.twitter.finagle.stats.StatsReceiver
import com.twitter.finatra.httpclient.modules.{HttpClientModule, HttpClientModuleTrait}
import com.twitter.finatra.jackson.ScalaObjectMapper
import com.twitter.finatra.jackson.modules.ScalaObjectMapperModule
import com.twitter.inject.app.TestInjector
import com.twitter.inject.modules.StatsReceiverModule
import com.twitter.inject.{Injector, InjectorModule, Test}

class HttpClientStartupIntegrationTest extends Test {

  test("startup non ssl with HttpClientModule") {
    val injector = TestInjector(
      modules =
        Seq(InjectorModule, StatsReceiverModule, ScalaObjectMapperModule, new HttpClientModule {
          override val dest = "flag!myservice"
        }),
      flags = Map("com.twitter.server.resolverMap" -> "myservice=nil!")
    ).create

    injector.instance[HttpClient]
  }

  test("startup ssl with HttpClientModule") {
    val injector = TestInjector(
      modules =
        Seq(InjectorModule, StatsReceiverModule, ScalaObjectMapperModule, new HttpClientModule {
          override val dest = "flag!myservice"
          override val sslHostname = Some("foo")
        }),
      flags = Map("com.twitter.server.resolverMap" -> "myservice=nil!")
    ).create

    injector.instance[HttpClient]
  }

  test("startup non ssl with HttpClientModuleTrait") {
    val injector = TestInjector(
      modules = Seq(
        InjectorModule,
        StatsReceiverModule,
        ScalaObjectMapperModule,
        new HttpClientModuleTrait {
          override val label: String = "test-non-ssl"
          override val dest: String = "flag!myservice"

          @Provides
          def providesHttpClient(
            injector: Injector,
            statsReceiver: StatsReceiver,
            mapper: ScalaObjectMapper
          ): HttpClient = newHttpClient(injector, statsReceiver, mapper)

        }
      ),
      flags = Map("com.twitter.server.resolverMap" -> "myservice=nil!")
    ).create

    injector.instance[HttpClient]
  }

  test("startup ssl with HttpClientModuleTrait") {
    val injector = TestInjector(
      modules = Seq(
        InjectorModule,
        StatsReceiverModule,
        ScalaObjectMapperModule,
        new HttpClientModuleTrait {
          override val label: String = "test-ssl"
          override val dest: String = "flag!myservice"

          override def configureClient(injector: Injector, client: Http.Client): Http.Client =
            client.withTls("foo")

          @Provides
          def providesHttpClient(
            injector: Injector,
            statsReceiver: StatsReceiver,
            mapper: ScalaObjectMapper
          ): HttpClient = newHttpClient(injector, statsReceiver, mapper)

        }
      ),
      flags = Map("com.twitter.server.resolverMap" -> "myservice=nil!")
    ).create

    injector.instance[HttpClient]
  }
}
