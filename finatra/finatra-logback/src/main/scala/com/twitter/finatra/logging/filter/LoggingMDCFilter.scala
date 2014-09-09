package com.twitter.finatra.logging.filter

import com.twitter.finagle.http.{Request, Response}
import com.twitter.finagle.tracing.Trace
import com.twitter.finagle.{Service, SimpleFilter}
import com.twitter.finatra.utils.Logging
import com.twitter.util.{Future, NonFatal}
import org.slf4j.{FinagleMDCInitializer, MDC}

/**
 * Set common fields into the logback logging MDC (Mapped Diagnostic Context)
 */
class LoggingMDCFilter
  extends SimpleFilter[Request, Response]
  with Logging {

  /* Initialize Finagle MDC adapter which overrides the standard Logback one */
  FinagleMDCInitializer.init()

  /* Public */

  def apply(request: Request, service: Service[Request, Response]): Future[Response] = {
    MDC.put("uri", request.uri)
    MDC.put("traceId", Trace.id.traceId.toString())
    addAdditionalFields(request)
    service(request).ensure {
      clearMdc()
    }
  }

  def addAdditionalFields(request: Request) {
  }

  /* Private */

  private def clearMdc() {
    try {
      MDC.clear()
    } catch {
      case NonFatal(e) =>
        error("Error clearing MDC", e)
    }
  }
}
