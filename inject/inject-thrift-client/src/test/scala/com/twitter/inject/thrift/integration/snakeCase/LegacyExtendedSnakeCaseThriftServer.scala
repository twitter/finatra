package com.twitter.inject.thrift.integration.snakeCase

import com.twitter.finagle.Filter
import com.twitter.finatra.annotations.DarkTrafficFilterType
import com.twitter.finatra.thrift.ThriftServer
import com.twitter.finatra.thrift.filters.{
  AccessLoggingFilter,
  ExceptionMappingFilter,
  LoggingMDCFilter,
  StatsFilter,
  ThriftMDCFilter,
  TraceIdMDCFilter
}
import com.twitter.finatra.thrift.routing.ThriftRouter

class LegacyExtendedSnakeCaseThriftServer extends ThriftServer {
  override val modules = Seq(new LegacyExtendedSnakeCaseThrfitServerDarkTrafficFilterModule)

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
      .add[LegacyExtendedSnakeCaseController]
}
