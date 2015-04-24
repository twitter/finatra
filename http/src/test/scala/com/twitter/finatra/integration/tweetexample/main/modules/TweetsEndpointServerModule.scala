package com.twitter.finatra.integration.tweetexample.main.modules

import com.twitter.finatra.integration.tweetexample.main.services.{MyTweetsRepository, TweetsRepository}
import com.twitter.inject.TwitterModule


object TweetsEndpointServerModule extends TwitterModule {

  override def configure() {
    bindSingleton[TweetsRepository].to[MyTweetsRepository]
  }
}
