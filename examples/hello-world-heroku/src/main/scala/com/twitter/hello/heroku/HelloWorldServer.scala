package com.twitter.hello.heroku

import com.twitter.finagle.http.{Request, Response}
import com.twitter.finatra.http.HttpServer
import com.twitter.finatra.http.filters.{CommonFilters, LoggingMDCFilter, TraceIdMDCFilter}
import com.twitter.finatra.http.routing.HttpRouter

object HelloWorldServerMain extends HelloWorldServer

class HelloWorldServer extends HttpServer {

  /*
   * Since Heroku only supports a single port per service,
   * we disable the Admin HTTP Server
   */
  override val disableAdminHttpServer = true

  override def configureHttp(router: HttpRouter) {
    router
      .filter[LoggingMDCFilter[Request, Response]]
      .filter[TraceIdMDCFilter[Request, Response]]
      .filter[CommonFilters]
      .add[HelloWorldController]
  }
}
