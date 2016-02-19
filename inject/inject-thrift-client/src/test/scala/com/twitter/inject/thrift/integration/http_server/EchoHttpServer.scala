package com.twitter.inject.thrift.integration.http_server

import com.twitter.finatra.http.HttpServer
import com.twitter.finatra.http.filters.CommonFilters
import com.twitter.finatra.http.routing.HttpRouter
import com.twitter.inject.thrift.modules.ThriftClientIdModule

class EchoHttpServer extends HttpServer {
  override val name = "echo-http-server"

  override val modules = Seq(
    ThriftClientIdModule,
    EchoThriftClientModule)

  override def configureHttp(router: HttpRouter) {
    router.
      filter[CommonFilters].
      add[EchoHttpController]
  }
}
