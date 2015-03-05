package com.twitter.inject.requestscope

import com.twitter.finagle.{Service, SimpleFilter}
import com.twitter.util.Future
import javax.inject.Inject

class FinagleRequestScopeFilter[Req, Rep] @Inject()(
  finagleScope: FinagleRequestScope)
  extends SimpleFilter[Req, Rep] {

  def apply(request: Req, service: Service[Req, Rep]): Future[Rep] = {
    finagleScope.enter()
    service.apply(request) ensure {
      finagleScope.exit()
    }
  }
}
