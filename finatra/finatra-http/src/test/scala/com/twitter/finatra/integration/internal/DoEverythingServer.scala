package com.twitter.finatra.integration.internal

import com.twitter.finatra.FinatraServer
import com.twitter.finatra.filters.CommonFilters
import com.twitter.finatra.twitterserver.routing.Router

object DoEverythingServerMain extends DoEverythingServer

class DoEverythingServer extends FinatraServer {

  override val name = "example-server"
  flag("magicNum", "26", "Magic number")

  override val modules = Seq(DoEverythingModule)

  override def configure(router: Router) {
    router.
      filter[CommonFilters].
      add[DoEverythingController]
  }

  override def warmup() {
    run[DoEverythingWarmupHandler]()
  }
}
