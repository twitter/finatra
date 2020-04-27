package com.twitter.finatra.example

import com.twitter.inject.TwitterModule

object QueueModule extends TwitterModule {

  override def configure(): Unit = {
    bindSingleton[Queue].toInstance(new Queue())
  }
}
