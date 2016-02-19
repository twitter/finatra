package com.twitter.inject.thrift.filtered_integration.http_server

import com.twitter.finatra.http.HttpServer
import com.twitter.finatra.http.filters.CommonFilters
import com.twitter.finatra.http.routing.HttpRouter
import com.twitter.inject.thrift.modules.ThriftClientIdModule

class GreeterHttpServer extends HttpServer {
  override val name = "greeter-server"

  override val modules = Seq(
    ThriftClientIdModule,
    GreeterThriftClientModule)

  override def configureHttp(router: HttpRouter) {
    router.
      filter[CommonFilters].
      add[GreeterHttpController]
  }
}
