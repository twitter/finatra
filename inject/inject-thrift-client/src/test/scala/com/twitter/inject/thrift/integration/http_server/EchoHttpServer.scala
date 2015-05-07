package com.twitter.inject.thrift.integration.http_server

import com.twitter.finatra.http.HttpServer
import com.twitter.finatra.http.filters.CommonFilters
import com.twitter.finatra.http.modules.StatsFilterModule
import com.twitter.finatra.http.routing.HttpRouter

class EchoHttpServer extends HttpServer {
  override val name = "echo-http-server"
  override val resolveFinagleClientsOnStartup = true

  override val modules = Seq(
    StatsFilterModule,
    EchoThriftClientModule)

  override def configureHttp(router: HttpRouter) {
    router.
      filter[CommonFilters].
      add[EchoHttpController]
  }
}
