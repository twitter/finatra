package com.twitter.finatra.thrift.tests.doeverything

import com.twitter.finatra.thrift.filters._
import com.twitter.finatra.thrift.modules.ClientIdWhitelistModule
import com.twitter.finatra.thrift.routing.ThriftRouter
import com.twitter.finatra.thrift.tests.doeverything.controllers.DoEverythingThriftController
import com.twitter.finatra.thrift.tests.doeverything.filters.ExceptionTranslationFilter
import com.twitter.finatra.thrift.{ThriftFilter, ThriftServer}
import com.twitter.inject.Logging
import com.twitter.inject.utils.Handler

object DoEverythingThriftServerMain extends DoEverythingThriftServer

class DoEverythingThriftServer extends ThriftServer {
  override val name = "example-server"

  flag("magicNum", "26", "Magic number")

  override val modules = Seq(
    ClientIdWhitelistModule)

  override def configureThrift(router: ThriftRouter): Unit = {
    router
      .filter[LoggingMDCFilter]
      .filter[TraceIdMDCFilter]
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
