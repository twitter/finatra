package com.twitter.finatra.thrift.filters

import com.twitter.finagle.Service
import com.twitter.finagle.tracing.Trace
import com.twitter.finatra.thrift.{ThriftRequest, ThriftFilter}
import javax.inject.Singleton
import org.slf4j.MDC

/**
 * Note: Any MDC filter must be used in conjunction with the LoggingMDCFilter
 * to ensure that diagnostic context is properly managed.
 */
@Singleton
class TraceIdMDCFilter extends ThriftFilter {

  override def apply[T, Rep](request: ThriftRequest[T], service: Service[ThriftRequest[T], Rep]) = {
    MDC.put("traceId", Trace.id.traceId.toString())
    service(request)
  }
}
