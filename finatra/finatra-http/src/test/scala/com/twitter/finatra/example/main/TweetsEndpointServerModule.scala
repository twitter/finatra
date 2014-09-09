package com.twitter.finatra.example.main

import com.twitter.finatra.example.main.services.{TweetsRepository, TweetyPieTweetsRepository}
import com.twitter.finatra.guice.GuiceModule


object TweetsEndpointServerModule extends GuiceModule {

  override def configure() {
    bindSingleton[TweetsRepository].to[TweetyPieTweetsRepository]
  }
}
