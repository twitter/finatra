package com.twitter.finatra.thrift.tests.doeverything

import com.twitter.finatra.logging.filter.{TypeAgnosticLoggingMDCFilter, TypeAgnosticTraceIdMDCFilter}
import com.twitter.finatra.logging.modules.Slf4jBridgeModule
import com.twitter.finatra.thrift.filters.{AccessLoggingFilter, ClientIdWhitelistFilter, StatsFilter, ThriftMDCFilter}
import com.twitter.finatra.thrift.modules.ClientIdWhitelistModule
import com.twitter.finatra.thrift.tests.doeverything.controllers.DoEverythingThriftController
import com.twitter.finatra.thrift.tests.doeverything.filters.ExceptionTranslationFilter
import com.twitter.finatra.thrift.{ThriftFilter, ThriftRouter, ThriftServer}
import com.twitter.inject.Logging
import com.twitter.inject.utils.Handler

object DoEverythingThriftServerMain extends DoEverythingThriftServer

class DoEverythingThriftServer extends ThriftServer {
  override val name = "example-server"

  flag("magicNum", "26", "Magic number")

  override val modules = Seq(
    Slf4jBridgeModule,
    ClientIdWhitelistModule)

  override def configureThrift(router: ThriftRouter): Unit = {
    router
      .typeAgnosticFilter[TypeAgnosticLoggingMDCFilter]
      .typeAgnosticFilter[TypeAgnosticTraceIdMDCFilter]
      .filter[ThriftMDCFilter]
      .filter(classOf[AccessLoggingFilter])
      .filter[StatsFilter]
      .filter[ExceptionTranslationFilter]
      .filter[ClientIdWhitelistFilter]
      .filter(ThriftFilter.Identity)
      .add[DoEverythingThriftController]
  }

  override def warmup() {
    run[DoEverythingThriftWarmupHandler]()
  }
}

class DoEverythingThriftWarmupHandler
  extends Handler
  with Logging {

  override def handle() = {
    // TODO: implement ThriftWarmup
    // warm up thrift service(s) here
    info("Warm-up done.")
  }
}
