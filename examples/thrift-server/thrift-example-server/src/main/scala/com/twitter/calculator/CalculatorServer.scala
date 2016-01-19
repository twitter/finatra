package com.twitter.calculator

import com.twitter.finatra.logging.filter.{TypeAgnosticLoggingMDCFilter, TypeAgnosticTraceIdMDCFilter}
import com.twitter.finatra.logging.modules.Slf4jBridgeModule
import com.twitter.finatra.thrift._
import com.twitter.finatra.thrift.filters.{AccessLoggingFilter, ClientIdWhitelistFilter, StatsFilter, ThriftMDCFilter}
import com.twitter.finatra.thrift.modules.ClientIdWhitelistModule

object CalculatorServerMain extends CalculatorServer

class CalculatorServer extends ThriftServer {
  override val name = "calculator-server"

  override def modules = Seq(
    Slf4jBridgeModule,
    ClientIdWhitelistModule)

  override def configureThrift(router: ThriftRouter) {
    router
      .typeAgnosticFilter[TypeAgnosticLoggingMDCFilter]
      .typeAgnosticFilter[TypeAgnosticTraceIdMDCFilter]
      .filter[ThriftMDCFilter]
      .filter[AccessLoggingFilter]
      .filter[StatsFilter]
      .filter[ExceptionTranslationFilter]
      .filter[ClientIdWhitelistFilter]
      .add[CalculatorController]
  }
}
