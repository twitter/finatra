package com.twitter.finatra.http.tests.integration.doeverything.main

import com.google.inject.Module
import com.twitter.finagle.Filter
import com.twitter.finagle.http.{Request, Response}
import com.twitter.finatra.http.filters.CommonFilters
import com.twitter.finatra.http.jsonpatch.{JsonPatchExceptionMapper, JsonPatchMessageBodyReader}
import com.twitter.finatra.http.routing.HttpRouter
import com.twitter.finatra.http.tests.integration.doeverything.main.controllers._
import com.twitter.finatra.http.tests.integration.doeverything.main.domain.DomainTestUserReader
import com.twitter.finatra.http.tests.integration.doeverything.main.exceptions.{BarExceptionMapper, FooBarBazExceptionMapper, FooExceptionMapper}
import com.twitter.finatra.http.tests.integration.doeverything.main.filters.{AppendToHeaderFilter, IdentityFilter}
import com.twitter.finatra.http.tests.integration.doeverything.main.modules.{DoEverythingModule, DoEverythingStatsReceiverModule}
import com.twitter.finatra.http.{Controller, HttpServer}
import com.twitter.finatra.httpclient.modules.HttpClientModule

object DoEverythingServerMain extends DoEverythingServer

class DoEverythingServer extends HttpServer {

  override val name = "doeverything-server"

  override def statsReceiverModule: Module = DoEverythingStatsReceiverModule

  flag("magicNum", "26", "Magic number")

  override val modules: Seq[Module] = Seq(
    new DoEverythingModule,
    new HttpClientModule {
      override val dest = "localhost:1234"
    }
  )

  override def configureHttp(router: HttpRouter): Unit = {
    router
      .register[JsonPatchMessageBodyReader]
      .register[DomainTestUserReader]
      .filter(new AppendToHeaderFilter("test", "0"), beforeRouting = true)
      .filter[CommonFilters]
      .filter(Filter.identity[Request, Response])
      .filter(new AppendToHeaderFilter("test", "1"))
      .add(new AppendToHeaderFilter("test", "2"), new ReadHeadersController)
      .add[DoEverythingController]
      .add(new NonGuiceController)
      .add[ForwardedController]
      .add(Filter.identity[Request, Response], new Controller {})
      .exceptionMapper[FooExceptionMapper]
      .exceptionMapper(injector.instance[BarExceptionMapper])
      .exceptionMapper[FooBarBazExceptionMapper]
      .exceptionMapper[JsonPatchExceptionMapper]
      .add[IdentityFilter, DoNothingController]
      .add[IdentityFilter, IdentityFilter, DoNothingController]
      .add[IdentityFilter, IdentityFilter, IdentityFilter, DoNothingController]
      .add[IdentityFilter, IdentityFilter, IdentityFilter, IdentityFilter, DoNothingController]
      .add[
        IdentityFilter,
        IdentityFilter,
        IdentityFilter,
        IdentityFilter,
        IdentityFilter,
        DoNothingController
      ]
      .add[
        IdentityFilter,
        IdentityFilter,
        IdentityFilter,
        IdentityFilter,
        IdentityFilter,
        IdentityFilter,
        DoNothingController
      ]
      .add[
        IdentityFilter,
        IdentityFilter,
        IdentityFilter,
        IdentityFilter,
        IdentityFilter,
        IdentityFilter,
        IdentityFilter,
        DoNothingController
      ]
      .add[
        IdentityFilter,
        IdentityFilter,
        IdentityFilter,
        IdentityFilter,
        IdentityFilter,
        IdentityFilter,
        IdentityFilter,
        IdentityFilter,
        DoNothingController
      ]
      .add[
        IdentityFilter,
        IdentityFilter,
        IdentityFilter,
        IdentityFilter,
        IdentityFilter,
        IdentityFilter,
        IdentityFilter,
        IdentityFilter,
        IdentityFilter,
        DoNothingController
      ]
      .add[
        IdentityFilter,
        IdentityFilter,
        IdentityFilter,
        IdentityFilter,
        IdentityFilter,
        IdentityFilter,
        IdentityFilter,
        IdentityFilter,
        IdentityFilter,
        IdentityFilter,
        DoNothingController
      ]
      .add[MustacheController]
  }

  override protected def warmup(): Unit = {
    handle[DoEverythingWarmupHandler]()
  }
}
