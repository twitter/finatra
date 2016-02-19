package com.twitter.finatra.multiserver.Add1HttpServer

import com.twitter.finatra.http.HttpServer
import com.twitter.finatra.http.filters.CommonFilters
import com.twitter.finatra.http.routing.HttpRouter
import com.twitter.finatra.thrift.ThriftClientExceptionMapper

class Add1Server extends HttpServer {
  override val modules = Seq(AdderThriftClientModule)

  override def configureHttp(router: HttpRouter) {
    router
      .exceptionMapper[ThriftClientExceptionMapper]
      .filter[CommonFilters]
      .add[Add1Controller]
  }
}
