package com.twitter.tiny

import com.twitter.finagle.http.{Request, Response}
import com.twitter.finatra.http.HttpServer
import com.twitter.finatra.http.filters.CommonFilters
import com.twitter.finatra.http.routing.HttpRouter
import com.twitter.finatra.logging.filter.{LoggingMDCFilter, TraceIdMDCFilter}
import com.twitter.finatra.logging.modules.LogbackModule
import com.twitter.tiny.exceptions.MalformedURLExceptionMapper
import com.twitter.tiny.modules.{JedisClientModule, ServicesModule, TinyUrlModule}

object TinyUrlServerMain extends TinyUrlServer

class TinyUrlServer extends HttpServer {
  override def modules = Seq(
    LogbackModule,
    ServicesModule,
    new TinyUrlModule)

  override def configureHttp(router: HttpRouter) {
    router.
      filter[LoggingMDCFilter[Request, Response]].
      filter[TraceIdMDCFilter[Request, Response]].
      filter[CommonFilters].
      add[TinyUrlController].
      exceptionMapper[MalformedURLExceptionMapper]
  }
}
