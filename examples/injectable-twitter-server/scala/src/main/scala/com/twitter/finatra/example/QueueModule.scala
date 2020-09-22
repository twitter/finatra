package com.twitter.finatra.example

import com.google.inject.{Provides, Singleton}
import com.twitter.inject.TwitterModule
import com.twitter.inject.annotations.Flag

object QueueModule extends TwitterModule {
  flag[Int]("max.queue.size", 6144, "Maximum queue size")

  @Provides
  @Singleton
  def provideQueue(@Flag("max.queue.size") maxQueueSize: Int): Queue =
    new Queue(maxQueueSize)
}
