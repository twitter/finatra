package com.twitter.inject.thrift.integration

import com.twitter.finagle.Filter
import com.twitter.finatra.annotations.DarkTrafficFilterType
import com.twitter.finatra.thrift.ThriftServer
import com.twitter.finatra.thrift.filters.{AccessLoggingFilter, ExceptionMappingFilter, LoggingMDCFilter, StatsFilter, ThriftMDCFilter, TraceIdMDCFilter}
import com.twitter.finatra.thrift.routing.ThriftRouter
import com.twitter.inject.thrift.integration.controllers.EchoController
import com.twitter.inject.thrift.integration.modules.DoEverythingThriftServerDarkTrafficFilterModule

class DarkTrafficThriftServer extends ThriftServer {

  override val modules = Seq(DoEverythingThriftServerDarkTrafficFilterModule)

  /**
   * Users MUST provide an implementation to configure the provided [[ThriftRouter]]. The [[ThriftRouter]]
   * exposes a DSL which results in a configured Finagle `Service[-Req, +Rep]` to serve on
   * the [[com.twitter.finagle.ListeningServer]].
   *
   * @param router the [[ThriftRouter]] to configure.
   */
  override protected def configureThrift(router: ThriftRouter): Unit =
    router
      .filter[LoggingMDCFilter]
      .filter[TraceIdMDCFilter]
      .filter[ThriftMDCFilter]
      .filter(classOf[AccessLoggingFilter])
      .filter[StatsFilter]
      .filter[ExceptionMappingFilter]
      .filter(Filter.TypeAgnostic.Identity)
      .filter[Filter.TypeAgnostic, DarkTrafficFilterType]
      .add[EchoController]
}
