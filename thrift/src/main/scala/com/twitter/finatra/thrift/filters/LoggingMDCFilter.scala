package com.twitter.finatra.thrift.filters

import com.twitter.finagle.Service
import com.twitter.finatra.thrift.{ThriftFilter, ThriftRequest}
import com.twitter.inject.logging.MDCInitializer
import com.twitter.util.Future
import javax.inject.Singleton

@Singleton
class LoggingMDCFilter extends ThriftFilter {

  /* Initialize MDC adapter which overrides the standard one */
  MDCInitializer.init()

  override def apply[T, Rep](
    request: ThriftRequest[T],
    service: Service[ThriftRequest[T], Rep]
  ): Future[Rep] = {
    MDCInitializer.let(service(request))
  }
}
