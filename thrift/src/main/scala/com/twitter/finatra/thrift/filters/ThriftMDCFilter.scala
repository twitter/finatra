package com.twitter.finatra.thrift.filters

import com.twitter.finagle.{Filter, Service}
import com.twitter.finagle.thrift.MethodMetadata
import com.twitter.finagle.thrift.ClientId
import com.twitter.util.Future
import javax.inject.Singleton
import org.slf4j.MDC

/**
 * Note: Any MDC filter must be used in conjunction with the LoggingMDCFilter
 * to ensure that diagnostic context is properly managed.
 */
@Singleton
class ThriftMDCFilter extends Filter.TypeAgnostic {
  def toFilter[T, U]: Filter[T, U, T, U] = new Filter[T, U, T, U] {
    def apply(
      request: T,
      service: Service[T, U]
    ): Future[U] = {
      for (meta <- MethodMetadata.current) {
        MDC.put("method", meta.methodName)
      }

      for (id <- ClientId.current) {
        MDC.put("clientId", id.name)
      }

      service(request)
    }
  }
}
