package com.twitter.finatra.http.tests.integration.multiserver.add2server

import com.google.inject.Provides
import com.twitter.finagle.stats.StatsReceiver
import com.twitter.finatra.httpclient.HttpClient
import com.twitter.finatra.httpclient.modules.HttpClientModuleTrait
import com.twitter.finatra.jackson.ScalaObjectMapper
import com.twitter.inject.Injector
import javax.inject.Singleton

object Add1HttpClientModule extends HttpClientModuleTrait {
  val dest = "flag!add1-server"
  val label = "add1-server"

  @Singleton
  @Provides
  final def provideHttpClient(
    injector: Injector,
    statsReceiver: StatsReceiver,
    mapper: ScalaObjectMapper
  ): HttpClient = newHttpClient(injector, statsReceiver, mapper)
}
