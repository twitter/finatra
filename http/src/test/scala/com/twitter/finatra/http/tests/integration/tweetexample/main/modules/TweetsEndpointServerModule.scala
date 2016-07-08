package com.twitter.finatra.http.tests.integration.tweetexample.main.modules

import com.twitter.finatra.http.tests.integration.tweetexample.main.services.{MyTweetsRepository, TweetsRepository}
import com.twitter.inject.TwitterModule


object TweetsEndpointServerModule extends TwitterModule {

  override def configure() {
    bindSingleton[TweetsRepository].to[MyTweetsRepository]
  }
}
