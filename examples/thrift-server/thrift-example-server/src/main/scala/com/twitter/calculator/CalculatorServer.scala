package com.twitter.calculator

import com.twitter.finatra.thrift.ThriftServer
import com.twitter.finatra.thrift.routing.ThriftRouter
import com.twitter.finatra.thrift.filters._
import com.twitter.finatra.thrift.modules.ClientIdWhitelistModule

object CalculatorServerMain extends CalculatorServer

class CalculatorServer extends ThriftServer {
  override val name = "calculator-server"

  override def modules = Seq(
    ClientIdWhitelistModule)

  override def configureThrift(router: ThriftRouter) {
    router
      .filter[LoggingMDCFilter]
      .filter[TraceIdMDCFilter]
      .filter[ThriftMDCFilter]
      .filter[AccessLoggingFilter]
      .filter[StatsFilter]
      .filter[ExceptionTranslationFilter]
      .filter[ClientIdWhitelistFilter]
      .add[CalculatorController]
  }
}
