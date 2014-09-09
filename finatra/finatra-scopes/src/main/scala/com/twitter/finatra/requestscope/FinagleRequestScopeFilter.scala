package com.twitter.finatra.requestscope

import com.twitter.finagle.http.{Request, Response}
import com.twitter.finagle.{Service, SimpleFilter}
import javax.inject.Inject

class FinagleRequestScopeFilter @Inject()(
  finagleScope: FinagleRequestScope)
  extends SimpleFilter[Request, Response] {

  def apply(request: Request, service: Service[Request, Response]) = {
    finagleScope.enter()

    addPathURL(request)

    service.apply(request) ensure {
      finagleScope.exit()
    }
  }

  /*
   * TODO: Move to another filter
   * TODO: Note: If we can't create the pathURL due to a missing Host header, we silently continue.
   * This allows us to handle requests without host headers, and only fail if/when the PathURL is needed.
   */
  private def addPathURL(request: Request) {
    for (pathURL <- PathURL.create(request)) {
      finagleScope.add(pathURL)
    }
  }
}
