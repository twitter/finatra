package com.twitter.finatra.multiserver.Add2HttpServer

import com.google.inject.Provides
import com.twitter.finagle.stats.StatsReceiver
import com.twitter.finatra.httpclient.HttpClient
import com.twitter.finatra.httpclient.modules.HttpClientModuleTrait
import com.twitter.inject.Injector
import com.twitter.util.jackson.ScalaObjectMapper
import javax.inject.Singleton

object Add1HttpClientModule extends HttpClientModuleTrait {
  val dest = "flag!add1-http-server"
  val label = "add1-http-server"

  @Singleton
  @Provides
  final def provideHttpClient(
    injector: Injector,
    statsReceiver: StatsReceiver,
    mapper: ScalaObjectMapper
  ): HttpClient = newHttpClient(injector, statsReceiver, mapper)
}
