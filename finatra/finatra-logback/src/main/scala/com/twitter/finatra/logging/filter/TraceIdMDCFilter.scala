package com.twitter.finatra.logging.filter

import com.twitter.finagle.tracing.Trace
import com.twitter.finagle.{Service, SimpleFilter}
import com.twitter.util.Future
import org.slf4j.MDC

class TraceIdMDCFilter[Req, Rep] extends SimpleFilter[Req, Rep] {

  override def apply(request: Req, service: Service[Req, Rep]): Future[Rep] = {
    MDC.put("traceId", Trace.id.traceId.toString())
    service(request)
  }
}
