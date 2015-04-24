package com.twitter.finatra.logging.integration

import com.twitter.finagle.http.{Request, Response}
import com.twitter.finatra.HttpServer
import com.twitter.finatra.filters.CommonFilters
import com.twitter.finatra.logging.filter.LoggingMDCFilter
import com.twitter.finatra.routing.HttpRouter

class PooledServer extends HttpServer {

  override def configureHttp(router: HttpRouter) {
    router.
      filter[LoggingMDCFilter[Request, Response]].
      filter[CommonFilters].
      add[PooledController]
  }
}
