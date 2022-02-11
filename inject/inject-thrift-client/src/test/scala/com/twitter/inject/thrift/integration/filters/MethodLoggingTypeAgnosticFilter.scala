package com.twitter.inject.thrift.integration.filters

import com.twitter.finagle.Filter
import com.twitter.finagle.Service
import com.twitter.util.Future
import com.twitter.util.logging.Logging

class MethodLoggingTypeAgnosticFilter extends Filter.TypeAgnostic with Logging {

  def toFilter[Req, Rep]: Filter[Req, Rep, Req, Rep] = new Filter[Req, Rep, Req, Rep] {
    def apply(request: Req, service: Service[Req, Rep]): Future[Rep] = {
      info("Method called with request " + request)
      service(request)
    }
  }
}
