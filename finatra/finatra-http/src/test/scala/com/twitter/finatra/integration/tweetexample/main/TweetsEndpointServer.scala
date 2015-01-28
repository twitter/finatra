package com.twitter.finatra.integration.tweetexample.main

import com.twitter.finatra.FinatraServer
import com.twitter.finatra.filters.CommonFilters
import com.twitter.finatra.integration.tweetexample.main.controllers.{AdminController, TweetsController}
import com.twitter.finatra.integration.tweetexample.main.domain.{BarCar, FooCar, CarMessageBodyWriter, TweetMessageBodyWriter}
import com.twitter.finatra.integration.tweetexample.main.filters.AuthFilter
import com.twitter.finatra.integration.tweetexample.main.modules.{AdminModule, TweetsEndpointServerModule}
import com.twitter.finatra.routing.Router

object TweetsEndpointServerMain extends TweetsEndpointServer

class TweetsEndpointServer extends FinatraServer {

  override val modules = Seq(
    TweetsEndpointServerModule,
    AdminModule)

  override def configure(router: Router) {
    router.
      register[TweetMessageBodyWriter].
      register[CarMessageBodyWriter, FooCar].
      register[CarMessageBodyWriter, BarCar].
      filter[CommonFilters].
      add[AuthFilter, TweetsController].
      add[AdminController]
  }
}
