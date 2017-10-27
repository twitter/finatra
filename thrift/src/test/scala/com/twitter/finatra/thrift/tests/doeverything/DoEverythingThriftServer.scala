package com.twitter.finatra.thrift.tests.doeverything

import com.twitter.finagle.ThriftMux
import com.twitter.finagle.tracing.NullTracer
import com.twitter.finatra.annotations.DarkTrafficFilterType
import com.twitter.finatra.thrift.exceptions.FinatraThriftExceptionMapper
import com.twitter.finatra.thrift.filters._
import com.twitter.finatra.thrift.modules.ClientIdWhitelistModule
import com.twitter.finatra.thrift.routing.ThriftRouter
import com.twitter.finatra.thrift.tests.doeverything.controllers.DoEverythingThriftController
import com.twitter.finatra.thrift.tests.doeverything.exceptions.{DoEverythingExceptionMapper, BarExceptionMapper, FooExceptionMapper}
import com.twitter.finatra.thrift.tests.doeverything.modules.DoEverythingThriftServerDarkTrafficFilterModule
import com.twitter.finatra.thrift.{ThriftFilter, ThriftServer}
import com.twitter.util.NullMonitor

object DoEverythingThriftServerMain extends DoEverythingThriftServer

class DoEverythingThriftServer extends ThriftServer {
  override val name = "example-server"

  flag("magicNum", "26", "Magic number")

  override val modules =
    Seq(ClientIdWhitelistModule, new DoEverythingThriftServerDarkTrafficFilterModule)

  override protected def configureThriftServer(server: ThriftMux.Server): ThriftMux.Server = {
    server
      .withMonitor(NullMonitor)
      .withTracer(NullTracer)
  }

  override def configureThrift(router: ThriftRouter): Unit = {
    router
      .filter[LoggingMDCFilter]
      .filter[TraceIdMDCFilter]
      .filter[ThriftMDCFilter]
      .filter(classOf[AccessLoggingFilter])
      .filter[StatsFilter]
      .filter[ExceptionMappingFilter]
      .filter[ClientIdWhitelistFilter]
      .filter(ThriftFilter.Identity)
      .filter[ThriftFilter, DarkTrafficFilterType]
      .exceptionMapper[FinatraThriftExceptionMapper]
      .exceptionMapper[BarExceptionMapper]
      .exceptionMapper[FooExceptionMapper]
      .exceptionMapper[DoEverythingExceptionMapper]
      .add[DoEverythingThriftController]
  }

  override def warmup() {
    handle[DoEverythingThriftWarmupHandler]()
  }
}
