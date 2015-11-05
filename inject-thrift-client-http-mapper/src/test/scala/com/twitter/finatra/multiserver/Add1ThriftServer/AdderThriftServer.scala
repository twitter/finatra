package com.twitter.finatra.multiserver.Add1ThriftServer

import com.twitter.finatra.logging.filter.{TraceIdMDCFilter, LoggingMDCFilter}
import com.twitter.finatra.thrift.filters.{AccessLoggingFilter, StatsFilter, ThriftMDCFilter}
import com.twitter.finatra.thrift.{ThriftRequest, ThriftRouter, ThriftServer}

class AdderThriftServer extends ThriftServer {
  override def configureThrift(router: ThriftRouter) {
    router
      .filter[LoggingMDCFilter[ThriftRequest, Any]]
      .filter[TraceIdMDCFilter[ThriftRequest, Any]]
      .filter[ThriftMDCFilter]
      .filter[AccessLoggingFilter]
      .filter[StatsFilter]
      .add[AdderImpl](FilteredAdder.create)
  }
}
