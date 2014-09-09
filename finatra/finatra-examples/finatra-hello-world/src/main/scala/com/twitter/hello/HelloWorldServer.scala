package com.twitter.hello

import com.twitter.finatra.FinatraServer
import com.twitter.finatra.filters.CommonFilters
import com.twitter.finatra.twitterserver.routing.Router

object HelloWorldServerMain extends HelloWorldServer

class HelloWorldServer extends FinatraServer {
  override val name = "hello-world"

  override def configure(router: Router) {
    router.
      filter[CommonFilters].
      add[HelloWorldController]
  }
}
