package com.twitter.finatra.integration.tweetexample.main

import com.twitter.finatra.HttpServer
import com.twitter.finatra.filters.CommonFilters
import com.twitter.finatra.integration.tweetexample.main.controllers.{AdminController, TweetsController}
import com.twitter.finatra.integration.tweetexample.main.domain.{BarCar, CarMessageBodyWriter, FooCar, TweetMessageBodyReader, TweetMessageBodyWriter}
import com.twitter.finatra.integration.tweetexample.main.filters.AuthFilter
import com.twitter.finatra.integration.tweetexample.main.modules.{AdminModule, TweetsEndpointServerModule}
import com.twitter.finatra.routing.HttpRouter

object TweetsEndpointServerMain extends TweetsEndpointServer

class TweetsEndpointServer extends HttpServer {

  override val modules = Seq(
    TweetsEndpointServerModule,
    AdminModule)

  override def configureHttp(router: HttpRouter) {
    router.
      register[TweetMessageBodyWriter].
      register[TweetMessageBodyReader].
      register[CarMessageBodyWriter, FooCar].
      register[CarMessageBodyWriter, BarCar].
      filter[CommonFilters].
      add[AuthFilter, TweetsController].
      add[AdminController]
  }
}
