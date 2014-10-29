package com.twitter.finatra.logging.filter

import com.twitter.finagle.http.{Request, Response}
import com.twitter.finagle.{Service, SimpleFilter}
import com.twitter.util.Future
import org.slf4j.{FinagleMDCInitializer, MDC}

/**
 * Set common Request fields into the logback MDC (Mapped Diagnostic Context)
 * @see http://logback.qos.ch/manual/mdc.html
 */
class LoggingMDCFilter extends SimpleFilter[Request, Response] {

  /* Initialize Finagle MDC adapter which overrides the standard Logback one */
  FinagleMDCInitializer.init()

  override def apply(request: Request, service: Service[Request, Response]): Future[Response] = {
    MDC.put("uri", request.uri)
    service(request).ensure {
      MDC.clear()
    }
  }
}
