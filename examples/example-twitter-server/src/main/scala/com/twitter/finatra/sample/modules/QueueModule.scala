package com.twitter.finatra.sample.modules

import com.twitter.finatra.sample.Queue
import com.twitter.inject.TwitterModule

object QueueModule extends TwitterModule {

  override def configure(): Unit = {
    bindSingleton[Queue].toInstance(new Queue())
  }
}
