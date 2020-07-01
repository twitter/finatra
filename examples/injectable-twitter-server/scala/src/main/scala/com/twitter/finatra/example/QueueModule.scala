package com.twitter.finatra.example

import com.twitter.inject.TwitterModule

object QueueModule extends TwitterModule {

  override def configure(): Unit = {
    bind[Queue].toInstance(new Queue())
  }
}
