package com.twitter.finatra.thrift.tests.doeverything

import com.twitter.finagle.{Filter, ThriftMux}
import com.twitter.finagle.tracing.NullTracer
import com.twitter.finatra.annotations.DarkTrafficFilterType
import com.twitter.finatra.thrift.exceptions.FinatraThriftExceptionMapper
import com.twitter.finatra.thrift.filters._
import com.twitter.finatra.thrift.modules.ClientIdAcceptlistModule
import com.twitter.finatra.thrift.routing.ThriftRouter
import com.twitter.finatra.thrift.tests.doeverything.controllers.DoEverythingThriftController
import com.twitter.finatra.thrift.tests.doeverything.exceptions.{BarExceptionMapper, DoEverythingExceptionMapper, FooExceptionMapper}
import com.twitter.finatra.thrift.tests.doeverything.modules.DoEverythingThriftServerDarkTrafficFilterModule
import com.twitter.finatra.thrift.ThriftServer
import com.twitter.util.NullMonitor

object DoEverythingThriftServerMain extends DoEverythingThriftServer

class DoEverythingThriftServer extends ThriftServer {
  override val name = "example-server"

  flag("magicNum", "26", "Magic number")

  override val modules =
    Seq(
      new ClientIdAcceptlistModule("/clients.yml"),
      new DoEverythingThriftServerDarkTrafficFilterModule)

  override protected def configureThriftServer(server: ThriftMux.Server): ThriftMux.Server = {
    server
      .withMonitor(NullMonitor)
      .withTracer(NullTracer)
      .withPerEndpointStats
  }

  override def configureThrift(router: ThriftRouter): Unit = {
    router
      .filter[LoggingMDCFilter]
      .filter[TraceIdMDCFilter]
      .filter[ThriftMDCFilter]
      .filter(classOf[AccessLoggingFilter])
      .filter[StatsFilter]
      .filter[ExceptionMappingFilter]
      .filter[ClientIdAcceptlistFilter]
      .filter(Filter.TypeAgnostic.Identity)
      .filter[Filter.TypeAgnostic, DarkTrafficFilterType]
      .exceptionMapper[FinatraThriftExceptionMapper]
      .exceptionMapper[BarExceptionMapper]
      .exceptionMapper[FooExceptionMapper]
      .exceptionMapper[DoEverythingExceptionMapper]
      .add[DoEverythingThriftController]
  }

  override protected def warmup(): Unit = {
    handle[DoEverythingThriftWarmupHandler]()
  }
}
