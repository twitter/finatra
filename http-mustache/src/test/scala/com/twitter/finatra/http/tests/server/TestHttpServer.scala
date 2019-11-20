package com.twitter.finatra.http.tests.server

import com.google.inject.Module
import com.twitter.finagle.http.Request
import com.twitter.finatra.http.HttpServer
import com.twitter.finatra.http.filters.{ExceptionMappingFilter, HttpResponseFilter}
import com.twitter.finatra.http.modules.MustacheModule
import com.twitter.finatra.http.routing.HttpRouter
import com.twitter.finatra.http.tests.server.controllers.TestController

class TestHttpServer extends HttpServer {

  override val modules: Seq[Module] = Seq(MustacheModule)

  override def configureHttp(router: HttpRouter): Unit = {
    router
      .filter[HttpResponseFilter[Request]]
      .filter[ExceptionMappingFilter[Request]]
      .add[TestController]
  }

  override protected def warmup(): Unit = { /* do nothing */ }
}
