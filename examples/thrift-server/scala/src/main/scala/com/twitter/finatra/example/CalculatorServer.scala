package com.twitter.finatra.example

import com.twitter.finatra.thrift.ThriftServer
import com.twitter.finatra.thrift.routing.ThriftRouter
import com.twitter.finatra.thrift.filters._

object CalculatorServerMain extends CalculatorServer

class CalculatorServer extends ThriftServer {
  override val name = "calculator-server"

  override def configureThrift(router: ThriftRouter): Unit = {
    router
      .filter[LoggingMDCFilter]
      .filter[TraceIdMDCFilter]
      .filter[ThriftMDCFilter]
      .filter[AccessLoggingFilter]
      .filter[StatsFilter]
      .filter[ExceptionMappingFilter]
      .add[CalculatorController]
  }
}
