package com.twitter.tiny

import com.twitter.finagle.httpx.{Request, Response}
import com.twitter.finatra.http.HttpServer
import com.twitter.finatra.http.filters.CommonFilters
import com.twitter.finatra.http.routing.HttpRouter
import com.twitter.finatra.logging.filter.{LoggingMDCFilter, TraceIdMDCFilter}
import com.twitter.finatra.logging.modules.Slf4jBridgeModule
import com.twitter.tiny.exceptions.MalformedURLExceptionMapper
import com.twitter.tiny.modules.{ServicesModule, TinyUrlModule}

object TinyUrlServerMain extends TinyUrlServer

class TinyUrlServer extends HttpServer {
  /*
   * Since Heroku only supports a single port per service,
   * we disable the Admin HTTP Server
   */
  override val disableAdminHttpServer = true

  override def modules = Seq(
    Slf4jBridgeModule,
    ServicesModule,
    TinyUrlModule)

  override def configureHttp(router: HttpRouter) {
    router.
      filter[LoggingMDCFilter[Request, Response]].
      filter[TraceIdMDCFilter[Request, Response]].
      filter[CommonFilters].
      add[TinyUrlController].
      exceptionMapper[MalformedURLExceptionMapper]
  }
}
