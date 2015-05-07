package com.twitter.inject.thrift.internal

import javax.inject.Provider

import com.twitter.finagle.Service
import com.twitter.inject.Logging
import com.twitter.util.{Future, Memoize, NonFatal}
import org.aopalliance.intercept.{MethodInterceptor, MethodInvocation}

/**
 * Interceptor for thrift client methods
 */
class ThriftClientMethodInterceptor(
  label: String,
  service: Service[FinatraThriftClientRequest, Any],
  thriftClientFilterChainProvider: Provider[ThriftClientFilterChain])
  extends MethodInterceptor
  with Logging {

  // Note: Must be lazy since thriftClientFilterChainProvider is not available at class creation time
  private lazy val thriftClientFilterChain = thriftClientFilterChainProvider.get()
  private lazy val globalFilter = thriftClientFilterChain.filter
  private lazy val methodFilters = thriftClientFilterChain.methodFilters

  /* Public */

  override def invoke(invocation: MethodInvocation): AnyRef = {
    trace("Intercepted thrift client call: " + invocation.getMethod)
    try {
      val request = FinatraThriftClientRequest.create(label, invocation)
      val filterChain = lookupFilterChain(request.methodName)
      filterChain(request, service)
    } catch {
      case NonFatal(e) =>
        error("Interception error: ", e)
        Future.exception(e)
    }
  }

  /* Private */

  private val lookupFilterChain = Memoize { methodName: String =>
    methodFilters.get(methodName) map { methodFilter =>
      globalFilter andThen methodFilter
    } getOrElse {
      globalFilter
    }
  }
}
