package com.twitter.finatra.multiserver.Add1ThriftServer

import com.twitter.finatra.logging.filter.{TypeAgnosticTraceIdMDCFilter, TypeAgnosticLoggingMDCFilter}
import com.twitter.finatra.thrift.filters.{AccessLoggingFilter, StatsFilter, ThriftMDCFilter}
import com.twitter.finatra.thrift.{ThriftRequest, ThriftRouter, ThriftServer}

class AdderThriftServer extends ThriftServer {
  override def configureThrift(router: ThriftRouter) {
    router
      .typeAgnosticFilter[TypeAgnosticLoggingMDCFilter]
      .typeAgnosticFilter[TypeAgnosticTraceIdMDCFilter]
      .filter[ThriftMDCFilter]
      .filter[AccessLoggingFilter]
      .filter[StatsFilter]
      .add[AdderImpl](FilteredAdder.create)
  }
}
