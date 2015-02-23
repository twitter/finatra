package com.twitter.finatra.logging.filter

import com.twitter.finagle.http.{Request, Response}
import com.twitter.finagle.tracing.{Trace, TraceId}
import com.twitter.finagle.{Service, SimpleFilter}
import com.twitter.util.Future
import org.slf4j.{FinagleMDCInitializer, MDC}

class TraceIdMDCFilter extends SimpleFilter[Request, Response] {

  override def apply(request: Request, service: Service[Request, Response]): Future[Response] = {
    MDC.put("traceId", Trace.id.traceId.toString())
    service(request).ensure {
      MDC.clear()
    }
  }
}
