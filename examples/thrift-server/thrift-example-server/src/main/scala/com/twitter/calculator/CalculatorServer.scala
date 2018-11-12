package com.twitter.calculator

import com.twitter.finatra.thrift.exceptions.FinatraThriftExceptionMapper
import com.twitter.finatra.thrift.ThriftServer
import com.twitter.finatra.thrift.routing.ThriftRouter
import com.twitter.finatra.thrift.filters._
import com.twitter.finatra.thrift.modules.ClientIdAcceptlistModule

object CalculatorServerMain extends CalculatorServer

class CalculatorServer extends ThriftServer {
  override val name = "calculator-server"

  override def modules = Seq(new ClientIdAcceptlistModule("/clients.yml"))

  override def configureThrift(router: ThriftRouter): Unit = {
    router
      .filter[LoggingMDCFilter]
      .filter[TraceIdMDCFilter]
      .filter[ThriftMDCFilter]
      .filter[AccessLoggingFilter]
      .filter[StatsFilter]
      .filter[ExceptionMappingFilter]
      .filter[ClientIdAcceptlistFilter]
      .exceptionMapper[FinatraThriftExceptionMapper]
      .add[CalculatorController]
  }
}
