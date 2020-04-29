package com.twitter.finatra.http.filters

import com.twitter.finagle.tracing.Trace
import com.twitter.finagle.{Service, SimpleFilter}
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
class TraceIdMDCFilter[Req, Rep] extends SimpleFilter[Req, Rep] {

  override def apply(request: Req, service: Service[Req, Rep]): Future[Rep] = {
    MDC.put("traceId", Trace.id.traceId.toString)
    // If sampling decision is not made yet
    // consider the trace not sampled for this span scope
    MDC.put("traceSampled", Trace.id._sampled.getOrElse(false).toString)
    MDC.put("traceSpanId", Trace.id.spanId.toString)
    service(request)
  }
}
