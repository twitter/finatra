package com.twitter.finatra.multiserver.CombinedServer

import com.twitter.conversions.DurationOps._
import com.twitter.finagle.Filter
import com.twitter.finatra.http.HttpServer
import com.twitter.finatra.http.filters.CommonFilters
import com.twitter.finatra.http.routing.HttpRouter
import com.twitter.finatra.thrift.filters._
import com.twitter.finatra.thrift.routing.ThriftRouter
import com.twitter.finatra.thrift.ThriftServer
import com.twitter.util.Duration

object DoEverythingCombinedServerMain extends DoEverythingCombinedServer

class DoEverythingCombinedServer extends HttpServer with ThriftServer {

  flag("magicNum", "26", "Magic number")

  override val name = "do-everything-combined-server"

  override val failfastOnFlagsNotParsed = false

  override val defaultShutdownTimeout: Duration = 30.seconds
  override val defaultThriftShutdownTimeout: Duration = 45.seconds

  override protected def configureHttp(router: HttpRouter): Unit = {
    router
      .filter[CommonFilters]
      .add[DoEverythingCombinedController]
  }

  override protected def configureThrift(router: ThriftRouter): Unit = {
    router
      .filter[LoggingMDCFilter]
      .filter[TraceIdMDCFilter]
      .filter[ThriftMDCFilter]
      .filter(classOf[AccessLoggingFilter])
      .filter[StatsFilter]
      .filter(Filter.TypeAgnostic.Identity)
      .add[DoEverythingCombinedThriftController]
  }
}
