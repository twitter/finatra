package com.twitter.finatra.thrift.filters

import com.twitter.finagle.Service
import com.twitter.finatra.thrift.{ThriftRequest, ThriftFilter}
import javax.inject.Singleton
import org.slf4j.{FinagleMDCInitializer, MDC}

@Singleton
class LoggingMDCFilter extends ThriftFilter {

  /* Initialize Finagle MDC adapter which overrides the standard one */
  FinagleMDCInitializer.init()

  override def apply[T, Rep](request: ThriftRequest[T], service: Service[ThriftRequest[T], Rep]) = {
    service(request).ensure {
      MDC.clear()
    }
  }
}
