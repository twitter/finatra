package com.twitter.finatra.http.filters

import com.twitter.finagle.{Service, SimpleFilter}
import com.twitter.inject.logging.MDCInitializer
import com.twitter.util.Future
import javax.inject.Singleton

@Singleton
class LoggingMDCFilter[Req, Rep] extends SimpleFilter[Req, Rep] {

  /* Initialize MDC adapter which overrides the standard one */
  MDCInitializer.init()

  override def apply(request: Req, service: Service[Req, Rep]): Future[Rep] = {
    MDCInitializer.let(service(request))
  }
}
