package com.twitter.finatra.thrift.filters

import com.twitter.finagle.{Filter, Service}
import com.twitter.inject.logging.MDCInitializer
import com.twitter.util.Future
import jakarta.inject.Singleton

@Singleton
class LoggingMDCFilter extends Filter.TypeAgnostic {
  /* Initialize MDC adapter which overrides the standard one */
  MDCInitializer.init()
  def toFilter[T, U]: Filter[T, U, T, U] = new Filter[T, U, T, U] {
    def apply(
      request: T,
      service: Service[T, U]
    ): Future[U] = MDCInitializer.let(service(request))
  }
}
