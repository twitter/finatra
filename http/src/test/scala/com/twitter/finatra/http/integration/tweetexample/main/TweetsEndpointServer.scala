package com.twitter.finatra.http.integration.tweetexample.main

import com.twitter.finatra.http.HttpServer
import com.twitter.finatra.http.filters.CommonFilters
import com.twitter.finatra.http.integration.tweetexample.main.controllers.{AdminController, TweetsController}
import com.twitter.finatra.http.integration.tweetexample.main.domain.{BarCar, CarMessageBodyWriter, FooCar, TweetMessageBodyReader, TweetMessageBodyWriter}
import com.twitter.finatra.http.integration.tweetexample.main.filters.AuthFilter
import com.twitter.finatra.http.integration.tweetexample.main.modules.{AdminModule, TweetsEndpointServerModule}
import com.twitter.finatra.http.routing.HttpRouter

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
