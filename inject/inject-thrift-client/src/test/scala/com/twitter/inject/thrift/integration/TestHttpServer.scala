package com.twitter.inject.thrift.integration

import com.google.inject.Module
import com.twitter.finatra.http.{Controller, HttpServer}
import com.twitter.finatra.http.filters.CommonFilters
import com.twitter.finatra.http.routing.HttpRouter
import com.twitter.inject.TwitterModule
import com.twitter.inject.thrift.modules.ThriftClientIdModule

class TestHttpServer[C <: Controller: Manifest](
  serverName: String,
  serverModules: TwitterModule*
) extends HttpServer {
  override val name = serverName

  override val modules: Seq[Module] =
    Seq(ThriftClientIdModule) ++ serverModules

  override def configureHttp(router: HttpRouter): Unit = {
    router
      .filter[CommonFilters]
      .add[C]
  }
}
