package com.twitter.finatra.thrift.filters

import com.twitter.finagle.{Filter, Service}
import com.twitter.finagle.tracing.Trace
import com.twitter.util.Future
import javax.inject.Singleton
import org.slf4j.MDC

/**
 * Filter to log tracing data into MDC bag of attributes.
 * Includes traceSampled flag that indicates if the trace
 * is available via tracers
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
      MDC.put("traceId", Trace.id.traceId.toString)
      // If sampling decision is not made yet
      // consider the trace not sampled for this span scope
      MDC.put("traceSampled", Trace.id._sampled.getOrElse(false).toString)
      MDC.put("traceSpanId", Trace.id.spanId.toString)
      service(request)
    }
  }
}
