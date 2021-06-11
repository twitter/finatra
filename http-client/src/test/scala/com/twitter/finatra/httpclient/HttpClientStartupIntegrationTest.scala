package com.twitter.finatra.httpclient

import com.google.inject.Provides
import com.twitter.finagle.Http
import com.twitter.finagle.stats.StatsReceiver
import com.twitter.finatra.httpclient.modules.HttpClientModuleTrait
import com.twitter.finatra.jackson.modules.ScalaObjectMapperModule
import com.twitter.inject.app.TestInjector
import com.twitter.inject.modules.StatsReceiverModule
import com.twitter.inject.{Injector, Test}
import com.twitter.util.jackson.ScalaObjectMapper

class HttpClientStartupIntegrationTest extends Test {

  test("startup non ssl with HttpClientModuleTrait") {
    val plainHttpClientModule = new HttpClientModuleTrait {
      val label: String = "test-non-ssl"
      val dest: String = "flag!myservice"

      @Provides
      def providesHttpClient(
        injector: Injector,
        statsReceiver: StatsReceiver,
        mapper: ScalaObjectMapper
      ): HttpClient = newHttpClient(injector, statsReceiver, mapper)

    }

    val injector = TestInjector(
      modules = Seq(
        StatsReceiverModule,
        ScalaObjectMapperModule,
        plainHttpClientModule
      ),
      flags = Map("com.twitter.server.resolverMap" -> "myservice=nil!")
    ).create

    injector.instance[HttpClient]
  }

  test("startup ssl with HttpClientModuleTrait") {
    val sslHttpClientModule = new HttpClientModuleTrait {
      val label: String = "test-ssl"
      val dest: String = "flag!myservice"

      override def configureClient(injector: Injector, client: Http.Client): Http.Client =
        client.withTls("foo")

      @Provides
      def providesHttpClient(
        injector: Injector,
        statsReceiver: StatsReceiver,
        mapper: ScalaObjectMapper
      ): HttpClient = newHttpClient(injector, statsReceiver, mapper)
    }

    val injector = TestInjector(
      modules = Seq(
        StatsReceiverModule,
        ScalaObjectMapperModule,
        sslHttpClientModule
      ),
      flags = Map("com.twitter.server.resolverMap" -> "myservice=nil!")
    ).create

    injector.instance[HttpClient]
  }
}
