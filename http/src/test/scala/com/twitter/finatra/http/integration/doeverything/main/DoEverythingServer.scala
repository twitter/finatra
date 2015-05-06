package com.twitter.finatra.http.integration.doeverything.main

import com.twitter.finatra.http.HttpServer
import com.twitter.finatra.http.filters.CommonFilters
import com.twitter.finatra.http.integration.doeverything.main.controllers.{DoEverythingController, NonGuiceController}
import com.twitter.finatra.http.integration.doeverything.main.domain.DomainTestUserReader
import com.twitter.finatra.http.integration.doeverything.main.exceptions.{BarExceptionMapper, FooExceptionMapper}
import com.twitter.finatra.http.integration.doeverything.main.modules.DoEverythingModule
import com.twitter.finatra.http.routing.HttpRouter

object DoEverythingServerMain extends DoEverythingServer

class DoEverythingServer extends HttpServer {

  override val name = "example-server"
  override val resolveFinagleClientsOnStartup = true

  flag("magicNum", "26", "Magic number")

  override val modules = Seq(
    DoEverythingModule)

  override def configureHttp(router: HttpRouter) {
    router.
      register[DomainTestUserReader].
      filter[CommonFilters].
      add[DoEverythingController].
      add(new NonGuiceController).
      exceptionMapper[FooExceptionMapper].
      exceptionMapper(injector.instance[BarExceptionMapper])
 }

  override def warmup() {
    run[DoEverythingWarmupHandler]()
  }
}
