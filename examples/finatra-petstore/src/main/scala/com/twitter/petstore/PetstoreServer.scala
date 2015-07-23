package com.twitter.petstore

import com.google.inject.Singleton
import com.twitter.finagle.http.{Response, Request}
import com.twitter.finatra.http.HttpServer
import com.twitter.finatra.http.filters.CommonFilters
import com.twitter.finatra.http.routing.HttpRouter
import com.twitter.finatra.logging.filter.{TraceIdMDCFilter, LoggingMDCFilter}
import com.twitter.finatra.logging.modules.Slf4jBridgeModule
import com.twitter.inject.TwitterModule

object PetstoreServerMain extends PetstoreServer

class PetstoreServer extends HttpServer {
  override def modules = Seq(
    Slf4jBridgeModule,
    new TwitterModule() {
      override protected def configure(): Unit = {
        bind[PetstoreDb].in[Singleton]
      }
    }
  )

  override def configureHttp(router: HttpRouter) {
    router
      .filter[LoggingMDCFilter[Request, Response]]
      .filter[TraceIdMDCFilter[Request, Response]]
      .filter[CommonFilters]
      .add[PetstorePetController]
  }
}
