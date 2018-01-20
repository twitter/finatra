package com.twitter.inject.thrift.integration.filters

import com.twitter.finagle.{Filter, Service}
import com.twitter.inject.Logging
import com.twitter.util.Future

class MethodLoggingTypeAgnosticFilter extends Filter.TypeAgnostic with Logging {

  def toFilter[Req, Rep]: Filter[Req, Rep, Req, Rep] = new Filter[Req, Rep, Req, Rep] {
    def apply(request: Req, service: Service[Req, Rep]): Future[Rep] = {
      info("Method called with request " + request)
      service(request)
    }
  }
}
