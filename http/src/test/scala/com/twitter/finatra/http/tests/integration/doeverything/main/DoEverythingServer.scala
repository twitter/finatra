package com.twitter.finatra.http.tests.integration.doeverything.main

import com.twitter.finagle.Filter
import com.twitter.finagle.http.{Request, Response}
import com.twitter.finatra.http.filters.CommonFilters
import com.twitter.finatra.http.routing.HttpRouter
import com.twitter.finatra.http.tests.integration.doeverything.main.controllers.{DoEverythingController, DoNothingController, NonGuiceController, ReadHeadersController}
import com.twitter.finatra.http.tests.integration.doeverything.main.domain.DomainTestUserReader
import com.twitter.finatra.http.tests.integration.doeverything.main.exceptions.{BarExceptionMapper, FooExceptionMapper}
import com.twitter.finatra.http.tests.integration.doeverything.main.filters.{AppendToHeaderFilter, IdentityFilter}
import com.twitter.finatra.http.tests.integration.doeverything.main.modules.{DoEverythingModule, DoEverythingStatsReceiverModule}
import com.twitter.finatra.http.{Controller, HttpServer}

object DoEverythingServerMain extends DoEverythingServer

class DoEverythingServer extends HttpServer {

  override val name = "example-server"

  override def statsReceiverModule = DoEverythingStatsReceiverModule

  flag("magicNum", "26", "Magic number")

  override val modules = Seq(
    DoEverythingModule)

  override def configureHttp(router: HttpRouter) {
    router
      .register[DomainTestUserReader]
      .filter(new AppendToHeaderFilter("test", "0"), beforeRouting = true)
      .filter[CommonFilters]
      .filter(Filter.identity[Request, Response])
      .filter(new AppendToHeaderFilter("test", "1"))
      .add(new AppendToHeaderFilter("test", "2"), new ReadHeadersController)
      .add[DoEverythingController]
      .add(new NonGuiceController)
      .add(Filter.identity[Request, Response], new Controller {})
      .exceptionMapper[FooExceptionMapper]
      .exceptionMapper(injector.instance[BarExceptionMapper])
      .add[IdentityFilter, DoNothingController]
      .add[IdentityFilter, IdentityFilter, DoNothingController]
      .add[IdentityFilter, IdentityFilter, IdentityFilter, DoNothingController]
      .add[IdentityFilter, IdentityFilter, IdentityFilter, IdentityFilter, DoNothingController]
      .add[IdentityFilter, IdentityFilter, IdentityFilter, IdentityFilter, IdentityFilter, DoNothingController]
      .add[IdentityFilter, IdentityFilter, IdentityFilter, IdentityFilter, IdentityFilter, IdentityFilter, DoNothingController]
      .add[IdentityFilter, IdentityFilter, IdentityFilter, IdentityFilter, IdentityFilter, IdentityFilter, IdentityFilter, DoNothingController]
      .add[IdentityFilter, IdentityFilter, IdentityFilter, IdentityFilter, IdentityFilter, IdentityFilter, IdentityFilter, IdentityFilter, DoNothingController]
      .add[IdentityFilter, IdentityFilter, IdentityFilter, IdentityFilter, IdentityFilter, IdentityFilter, IdentityFilter, IdentityFilter, IdentityFilter, DoNothingController]
      .add[IdentityFilter, IdentityFilter, IdentityFilter, IdentityFilter, IdentityFilter, IdentityFilter, IdentityFilter, IdentityFilter, IdentityFilter, IdentityFilter, DoNothingController]
  }

  override def warmup() {
    handle[DoEverythingWarmupHandler]()
  }
}
