package com.twitter.finatra.thrift.filters

import com.twitter.finagle.{Filter, Service}
import com.twitter.finagle.tracing.Trace
import com.twitter.util.Future
import javax.inject.Singleton
import org.slf4j.MDC

/**
 * Note: Any MDC filter must be used in conjunction with the LoggingMDCFilter
 * to ensure that diagnostic context is properly managed.
 */
@Singleton
class TraceIdMDCFilter extends Filter.TypeAgnostic {
  def toFilter[T, U]: Filter[T, U, T, U] = new Filter[T, U, T, U] {
    def apply(
      request: T,
      service: Service[T, U]
    ): Future[U] = {
      MDC.put("traceId", Trace.id.traceId.toString())
      service(request)
    }
  }
}
