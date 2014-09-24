package com.twitter.finatra.requestscope

import com.twitter.finagle.http.{Request, Response}
import com.twitter.finagle.{Service, SimpleFilter}
import javax.inject.Inject

class FinagleRequestScopeFilter @Inject()(
  finagleScope: FinagleRequestScope)
  extends SimpleFilter[Request, Response] {

  def apply(request: Request, service: Service[Request, Response]) = {
    finagleScope.enter()
    service.apply(request) ensure {
      finagleScope.exit()
    }
  }
}
