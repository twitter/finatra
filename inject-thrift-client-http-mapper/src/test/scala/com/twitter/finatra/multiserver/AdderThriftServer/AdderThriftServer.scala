package com.twitter.finatra.multiserver.AdderThriftServer

import com.twitter.finatra.thrift.filters._
import com.twitter.finatra.thrift.ThriftServer
import com.twitter.finatra.thrift.routing.ThriftRouter

class AdderThriftServer extends ThriftServer {
  override val name = "adder-thrift-server"

  override def configureThrift(router: ThriftRouter): Unit = {
    router
      .filter[LoggingMDCFilter]
      .filter[TraceIdMDCFilter]
      .filter[ThriftMDCFilter]
      .filter[AccessLoggingFilter]
      .filter[StatsFilter]
      .add[AdderThriftController]
  }
}
