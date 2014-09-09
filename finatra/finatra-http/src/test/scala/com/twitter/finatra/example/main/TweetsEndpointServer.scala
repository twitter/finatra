package com.twitter.finatra.example.main

import com.twitter.finatra.FinatraServer
import com.twitter.finatra.example.main.controllers.TweetsController
import com.twitter.finatra.example.main.domain.{CarMessageBodyWriter, TweetMessageBodyWriter}
import com.twitter.finatra.filters.CommonFilters
import com.twitter.finatra.twitterserver.routing.Router

object TweetsEndpointServerMain extends TweetsEndpointServer

class TweetsEndpointServer extends FinatraServer {
  override val modules = Seq(
    TweetsEndpointServerModule)

  override def configure(router: Router) {
    router.
      register[TweetMessageBodyWriter].
      register[CarMessageBodyWriter].
      filter[CommonFilters].
      add[TweetsController]
  }
}
