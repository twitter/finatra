package com.twitter.calculator

import com.twitter.calculator.manual_codegen.FilteredCalculator
import com.twitter.finatra.logging.filter.{LoggingMDCFilter, TraceIdMDCFilter}
import com.twitter.finatra.logging.modules.Slf4jBridgeModule
import com.twitter.finatra.thrift.filters.{AccessLoggingFilter, ClientIdWhitelistFilter, StatsFilter, ThriftMDCFilter}
import com.twitter.finatra.thrift.modules.ClientIdWhitelistModule
import com.twitter.finatra.thrift.{ThriftRequest, _}

object CalculatorServerMain extends CalculatorServer

class CalculatorServer extends ThriftServer {
  override val name = "calculator-server"

  override def modules = Seq(
    Slf4jBridgeModule,
    ClientIdWhitelistModule)

  override def configureThrift(router: ThriftRouter) {
    router
      .filter[LoggingMDCFilter[ThriftRequest, Any]]
      .filter[TraceIdMDCFilter[ThriftRequest, Any]]
      .filter[ThriftMDCFilter]
      .filter[AccessLoggingFilter]
      .filter[StatsFilter]
      .filter[ExceptionTranslationFilter]
      .filter[ClientIdWhitelistFilter]
      .add[CalculatorImpl](FilteredCalculator.create)
  }
}
