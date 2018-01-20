package com.twitter.inject.thrift.integration.inheritance

import com.google.inject.Module
import com.twitter.finatra.http.HttpServer
import com.twitter.finatra.http.filters.CommonFilters
import com.twitter.finatra.http.routing.HttpRouter
import com.twitter.inject.thrift.modules.ThriftClientIdModule

class ServiceBHttpServer extends HttpServer {
  override val modules: Seq[Module] =
    Seq(ThriftClientIdModule, ServiceBServicePerEndpointModule)

  override def configureHttp(router: HttpRouter) {
    router
      .filter[CommonFilters]
      .add[ServiceBHttpController]
  }
}
