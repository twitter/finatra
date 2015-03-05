package com.twitter.finatra.integration.doeverything.main

import com.twitter.finatra.HttpServer
import com.twitter.finatra.filters.CommonFilters
import com.twitter.finatra.integration.doeverything.main.controllers.{DoEverythingController, NonGuiceController}
import com.twitter.finatra.integration.doeverything.main.domain.DomainTestUserReader
import com.twitter.finatra.integration.doeverything.main.modules.DoEverythingModule
import com.twitter.finatra.routing.HttpRouter

object DoEverythingServerMain extends DoEverythingServer

class DoEverythingServer extends HttpServer {

  override val name = "example-server"
  flag("magicNum", "26", "Magic number")

  override val modules = Seq(DoEverythingModule)

  override def configureHttp(router: HttpRouter) {
    router.
      register[DomainTestUserReader].
      filter[CommonFilters].
      add[DoEverythingController].
      add(new NonGuiceController)
  }

  override def warmup() {
    run[DoEverythingWarmupHandler]()
  }
}
