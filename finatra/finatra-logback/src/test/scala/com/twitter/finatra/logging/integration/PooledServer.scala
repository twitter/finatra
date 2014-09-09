package com.twitter.finatra.logging.integration

import com.twitter.finatra.FinatraServer
import com.twitter.finatra.filters.CommonFilters
import com.twitter.finatra.logging.filter.LoggingMDCFilter
import com.twitter.finatra.twitterserver.routing.Router

object PooledServerMain extends PooledServer

class PooledServer extends FinatraServer {

  override def configure(router: Router) {
    router.
      filter[LoggingMDCFilter].
      filter[CommonFilters].
      add[PooledController]
  }
}
