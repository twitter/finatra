package com.twitter.finatra.thrift.filters

import com.twitter.finagle.{Service, SimpleFilter}
import com.twitter.finatra.thrift.ThriftRequest
import com.twitter.util.Future
import javax.inject.Singleton
import org.slf4j.MDC

/**
 * Note: Any MDC filter must be used on conjunction with the LoggingMDCFilter
 * to ensure that diagnostic context is properly managed.
 */
@Singleton
class ThriftMDCFilter
  extends SimpleFilter[ThriftRequest, Any] {

  override def apply(request: ThriftRequest, service: Service[ThriftRequest, Any]): Future[Any] = {
    MDC.put("method", request.methodName)

    for (id <- request.clientId) {
      MDC.put("clientId", id.name)
    }

    service(request)
  }
}
