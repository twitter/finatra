package com.twitter.streaming

import com.twitter.finatra.http.HttpServer
import com.twitter.finatra.http.filters.CommonFilters
import com.twitter.finatra.http.routing.HttpRouter

object StreamingServerMain extends StreamingServer

class StreamingServer extends HttpServer {
  override def streamRequest = true

  override def configureHttp(router: HttpRouter) {
    router
      .filter[CommonFilters]
      .add[StreamingController]
  }
}
