package com.twitter.finatra.multiserver.Add2HttpServer

import com.twitter.finatra.http.HttpServer
import com.twitter.finatra.http.filters.CommonFilters
import com.twitter.finatra.http.routing.HttpRouter

class Add2Server extends HttpServer {
  override val modules = Seq(Add1HttpClientModule)

  override def configureHttp(router: HttpRouter) {
    router
      .filter[CommonFilters]
      .add[Add2Controller]
  }
}
