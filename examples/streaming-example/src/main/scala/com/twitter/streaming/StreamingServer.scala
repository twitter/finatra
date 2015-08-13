package com.twitter.streaming

import com.twitter.finatra.http.HttpServer
import com.twitter.finatra.http.filters.CommonFilters
import com.twitter.finatra.http.routing.HttpRouter
import com.twitter.finatra.logging.modules.Slf4jBridgeModule

object StreamingServerMain extends StreamingServer

class StreamingServer extends HttpServer {
  override def modules = Seq(Slf4jBridgeModule)
  override def streamRequest = true

  override def configureHttp(router: HttpRouter) {
    router
      .filter[CommonFilters]
      .add[StreamingController]
  }
}
