package com.twitter.finatra.integration.tweetexample.main.modules

import com.twitter.finatra.guice.GuiceModule
import com.twitter.finatra.integration.tweetexample.main.services.{MyTweetsRepository, TweetsRepository}


object TweetsEndpointServerModule extends GuiceModule {

  override def configure() {
    bindSingleton[TweetsRepository].to[MyTweetsRepository]
  }
}
