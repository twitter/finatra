package com.twitter.hello

import com.twitter.finagle.httpx.{Request, Response}
import com.twitter.finatra.http.HttpServer
import com.twitter.finatra.http.filters.CommonFilters
import com.twitter.finatra.http.routing.HttpRouter
import com.twitter.finatra.logging.filter.{LoggingMDCFilter, TraceIdMDCFilter}
import com.twitter.finatra.logging.modules.Slf4jBridgeModule

object HelloWorldServerMain extends HelloWorldServer

class HelloWorldServer extends HttpServer {

  /*
   * Since Heroku only supports a single port per service,
   * we disable the Admin HTTP Server
   */
  override val disableAdminHttpServer = true

  override def modules = Seq(Slf4jBridgeModule)

  override def configureHttp(router: HttpRouter) {
    router
      .filter[LoggingMDCFilter[Request, Response]]
      .filter[TraceIdMDCFilter[Request, Response]]
      .filter[CommonFilters]
      .add[HelloWorldController]
  }
}
