package com.twitter.finatra.http.filters

import com.twitter.finagle.{SimpleFilter, Service}
import com.twitter.util.Future
import javax.inject.Singleton
import org.slf4j.{FinagleMDCInitializer, MDC}

@Singleton
class LoggingMDCFilter[Req, Rep] extends SimpleFilter[Req, Rep] {

  /* Initialize Finagle MDC adapter which overrides the standard one */
  FinagleMDCInitializer.init()

  override def apply(request: Req, service: Service[Req, Rep]): Future[Rep] = {
    service(request).ensure {
      MDC.clear()
    }
  }

}
