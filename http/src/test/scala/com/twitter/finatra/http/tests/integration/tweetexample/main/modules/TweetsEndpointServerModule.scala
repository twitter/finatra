package com.twitter.finatra.http.tests.integration.tweetexample.main.modules

import com.twitter.finatra.http.tests.integration.tweetexample.main.services.{
  MyTweetsRepository,
  TweetsRepository
}
import com.twitter.inject.TwitterModule
import javax.inject.Singleton

object TweetsEndpointServerModule extends TwitterModule {

  override def configure(): Unit = {
    bind[TweetsRepository].to[MyTweetsRepository].in[Singleton]
  }
}
