package com.twitter.finatra.logging.filter

import com.twitter.finagle.tracing.Trace
import com.twitter.finagle.{Filter, Service, SimpleFilter}
import com.twitter.util.Future
import org.slf4j.MDC

class TraceIdMDCFilter[Req, Rep] extends SimpleFilter[Req, Rep] {

  override def apply(request: Req, service: Service[Req, Rep]): Future[Rep] = {
    MDC.put("traceId", Trace.id.traceId.toString())
    service(request)
  }
}

class TypeAgnosticTraceIdMDCFilter extends Filter.TypeAgnostic {
  override def toFilter[Req, Rep]: Filter[Req, Rep, Req, Rep] = new TraceIdMDCFilter[Req, Rep]
}