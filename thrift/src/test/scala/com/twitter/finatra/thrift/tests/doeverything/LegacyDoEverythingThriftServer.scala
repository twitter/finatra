package com.twitter.finatra.thrift.tests.doeverything

import com.twitter.finagle.{Filter, ThriftMux}
import com.twitter.finagle.tracing.NullTracer
import com.twitter.finatra.thrift.filters._
import com.twitter.finatra.thrift.routing.ThriftRouter
import com.twitter.finatra.thrift.tests.doeverything.controllers.LegacyDoEverythingThriftController
import com.twitter.finatra.thrift.tests.doeverything.exceptions.{
  BarExceptionMapper,
  DoEverythingExceptionMapper,
  FooExceptionMapper
}
import com.twitter.finatra.thrift.ThriftServer
import com.twitter.util.NullMonitor

object LegacyDoEverythingThriftServerMain extends LegacyDoEverythingThriftServer

@deprecated(
  "These tests exist to ensure legacy functionality still operates. Do not use them for guidance",
  "2018-12-20")
class LegacyDoEverythingThriftServer extends ThriftServer {
  override val name = "example-server"

  flag("magicNum", "26", "Magic number")

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
      .filter(Filter.TypeAgnostic.Identity)
      .exceptionMapper[BarExceptionMapper]
      .exceptionMapper[FooExceptionMapper]
      .exceptionMapper[DoEverythingExceptionMapper]
      .add[LegacyDoEverythingThriftController]
  }

  override protected def warmup(): Unit = {
    handle[DoEverythingThriftWarmupHandler]()
  }
}
