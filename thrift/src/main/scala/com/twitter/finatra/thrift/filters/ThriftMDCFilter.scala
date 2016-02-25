package com.twitter.finatra.thrift.filters

import com.twitter.finagle.Service
import com.twitter.finatra.thrift.{ThriftFilter, ThriftRequest}
import com.twitter.util.Future
import javax.inject.Singleton
import org.slf4j.MDC

/**
 * Note: Any MDC filter must be used in conjunction with the LoggingMDCFilter
 * to ensure that diagnostic context is properly managed.
 */
@Singleton
class ThriftMDCFilter extends ThriftFilter {

  override def apply[T, U](request: ThriftRequest[T], service: Service[ThriftRequest[T], U]): Future[U] = {
    MDC.put("method", request.methodName)

    for (id <- request.clientId) {
      MDC.put("clientId", id.name)
    }

    service(request)
  }
}
