package com.twitter.finatra.thrift.tests.doeverything

import com.twitter.finagle.{Filter, ThriftMux}
import com.twitter.finatra.thrift.filters._
import com.twitter.finatra.thrift.routing.ThriftRouter
import com.twitter.finatra.thrift.tests.doeverything.controllers.DoEverythingThriftController
import com.twitter.finatra.thrift.tests.doeverything.exceptions.{
  ReqRepBarExceptionMapper,
  ReqRepDoEverythingExceptionMapper,
  ReqRepFooExceptionMapper
}
import com.twitter.finatra.thrift.ThriftServer
import com.twitter.util.NullMonitor

object DoEverythingThriftServerMain extends DoEverythingThriftServer

class DoEverythingThriftServer extends ThriftServer {
  override val name = "example-server"

  flag("magicNum", "26", "Magic number")

  override protected def configureThriftServer(server: ThriftMux.Server): ThriftMux.Server = {
    server
      .withMonitor(NullMonitor)
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
      .exceptionMapper[ReqRepBarExceptionMapper]
      .exceptionMapper[ReqRepFooExceptionMapper]
      .exceptionMapper[ReqRepDoEverythingExceptionMapper]
      .add[DoEverythingThriftController]
  }

  override protected def warmup(): Unit = {
    handle[DoEverythingThriftWarmupHandler]()
  }
}
