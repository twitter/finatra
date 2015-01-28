package com.twitter.finatra.integration.doeverything.main

import com.twitter.finatra.FinatraServer
import com.twitter.finatra.filters.CommonFilters
import com.twitter.finatra.integration.doeverything.main.controllers.{DoEverythingController, NonGuiceController}
import com.twitter.finatra.integration.doeverything.main.modules.DoEverythingModule
import com.twitter.finatra.routing.Router

object DoEverythingServerMain extends DoEverythingServer

class DoEverythingServer extends FinatraServer {

  override val name = "example-server"
  flag("magicNum", "26", "Magic number")

  override val modules = Seq(DoEverythingModule)

  override def configure(router: Router) {
    router.
      filter[CommonFilters].
      add[DoEverythingController].
      add(new NonGuiceController)
  }

  override def warmup() {
    run[DoEverythingWarmupHandler]()
  }
}
