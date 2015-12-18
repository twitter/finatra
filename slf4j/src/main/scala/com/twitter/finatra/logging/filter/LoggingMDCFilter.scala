package com.twitter.finatra.logging.filter

import com.twitter.finagle.{Filter, Service, SimpleFilter}
import com.twitter.util.Future
import org.slf4j.{FinagleMDCInitializer, MDC}

/**
 * Initialize the Logback Mapped Diagnostic Context, and clear the MDC after each request
 * @see http://logback.qos.ch/manual/mdc.html
 */
class LoggingMDCFilter[Req, Rep] extends SimpleFilter[Req, Rep] {

  /* Initialize Finagle MDC adapter which overrides the standard Logback one */
  FinagleMDCInitializer.init()

  override def apply(request: Req, service: Service[Req, Rep]): Future[Rep] = {
    service(request).ensure {
      MDC.clear()
    }
  }
}

class TypeAgnosticLoggingMDCFilter extends Filter.TypeAgnostic {
  override def toFilter[Req, Rep]: Filter[Req, Rep, Req, Rep] = new LoggingMDCFilter[Req, Rep]
}