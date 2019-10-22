package com.twitter.finatra.http.tests.integration.darktraffic.main

import com.twitter.finagle.Filter
import com.twitter.finagle.http.{Request, Response}
import com.twitter.finatra.annotations.DarkTrafficFilterType
import com.twitter.finatra.http.HttpServer
import com.twitter.finatra.http.filters.CommonFilters
import com.twitter.finatra.http.routing.HttpRouter

object DarkTrafficTestServerMain extends DarkTrafficTestServer

class DarkTrafficTestServer extends HttpServer {

  override val name = "dark-traffic-service-example"

  override val modules = Seq(DarkServerTestModule)

  override def configureHttp(router: HttpRouter): Unit = {
    router
      .filter[CommonFilters]
      .filter[Filter[Request, Response, Request, Response], DarkTrafficFilterType]
      .add[DarkTrafficTestController]
  }
}
