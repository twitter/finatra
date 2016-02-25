package com.twitter.finatra.http.filters

import com.twitter.finagle.tracing.Trace
import com.twitter.finagle.{Service, SimpleFilter}
import javax.inject.Singleton
import org.slf4j.MDC

/**
 * Note: Any MDC filter must be used in conjunction with the LoggingMDCFilter
 * to ensure that diagnostic context is properly managed.
 */
@Singleton
class TraceIdMDCFilter[Req, Rep] extends SimpleFilter[Req, Rep] {

  override def apply(request: Req, service: Service[Req, Rep]) = {
    MDC.put("traceId", Trace.id.traceId.toString())
    service(request)
  }
}
