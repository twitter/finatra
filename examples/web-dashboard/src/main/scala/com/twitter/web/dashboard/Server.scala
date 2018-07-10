package com.twitter.web.dashboard

import com.twitter.finatra.http.HttpServer
import com.twitter.finatra.http.filters.CommonFilters
import com.twitter.finatra.http.routing.HttpRouter
import com.twitter.web.dashboard.controllers.DashboardController

object ServerMain extends Server

class Server extends HttpServer {
  override val name = "dashboard"

  override def configureHttp(router: HttpRouter): Unit = {
    router
      .filter[CommonFilters]
      .add[DashboardController]
  }
}
