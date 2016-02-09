package com.twitter.finatra.multiserver.CombinedServer

import com.twitter.conversions.time._
import com.twitter.finatra.http.HttpServer
import com.twitter.finatra.http.filters.CommonFilters
import com.twitter.finatra.http.routing.HttpRouter
import com.twitter.finatra.logging.filter.{TypeAgnosticLoggingMDCFilter, TypeAgnosticTraceIdMDCFilter}
import com.twitter.finatra.logging.modules.Slf4jBridgeModule
import com.twitter.finatra.thrift.filters.{AccessLoggingFilter, StatsFilter, ThriftMDCFilter}
import com.twitter.finatra.thrift.{ThriftFilter, ThriftRouter, ThriftServer}

object DoEverythingCombinedServerMain extends DoEverythingCombinedServer

class DoEverythingCombinedServer
  extends HttpServer
  with ThriftServer {

  flag("magicNum", "26", "Magic number")

  override val name = "do-everything-combined-server"

  override val disableAdminHttpServer = true

  override val failfastOnFlagsNotParsed = false

  override val defaultShutdownTimeout = 30.seconds
  override val defaultThriftShutdownTimeout = 45.seconds

  override val modules = Seq(
    Slf4jBridgeModule)

  override protected def configureHttp(router: HttpRouter) = {
    router
      .filter[CommonFilters]
      .add[DoEverythingCombinedController]
  }

  override protected def configureThrift(router: ThriftRouter) {
    router
      .typeAgnosticFilter[TypeAgnosticLoggingMDCFilter]
      .typeAgnosticFilter[TypeAgnosticTraceIdMDCFilter]
      .filter[ThriftMDCFilter]
      .filter(classOf[AccessLoggingFilter])
      .filter[StatsFilter]
      .filter(ThriftFilter.Identity)
      .add[DoEverythingCombinedThriftController]
  }
}
