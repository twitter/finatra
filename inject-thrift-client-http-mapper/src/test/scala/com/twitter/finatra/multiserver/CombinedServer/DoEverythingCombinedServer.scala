package com.twitter.finatra.multiserver.CombinedServer

import com.twitter.conversions.time._
import com.twitter.finatra.http.HttpServer
import com.twitter.finatra.http.filters.CommonFilters
import com.twitter.finatra.http.routing.HttpRouter
import com.twitter.finatra.thrift.filters._
import com.twitter.finatra.thrift.routing.ThriftRouter
import com.twitter.finatra.thrift.{ThriftFilter, ThriftServer}

object DoEverythingCombinedServerMain extends DoEverythingCombinedServer

class DoEverythingCombinedServer
  extends HttpServer
  with ThriftServer {

  flag("magicNum", "26", "Magic number")

  override val name = "do-everything-combined-server"

  override val failfastOnFlagsNotParsed = false

  override val defaultShutdownTimeout = 30.seconds
  override val defaultThriftShutdownTimeout = 45.seconds

  override protected def configureHttp(router: HttpRouter) = {
    router
      .filter[CommonFilters]
      .add[DoEverythingCombinedController]
  }

  override protected def configureThrift(router: ThriftRouter) {
    router
      .filter[LoggingMDCFilter]
      .filter[TraceIdMDCFilter]
      .filter[ThriftMDCFilter]
      .filter(classOf[AccessLoggingFilter])
      .filter[StatsFilter]
      .filter(ThriftFilter.Identity)
      .add[DoEverythingCombinedThriftController]
  }
}
