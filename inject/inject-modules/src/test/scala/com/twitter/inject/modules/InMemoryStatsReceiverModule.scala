package com.twitter.inject.modules

import com.twitter.finagle.stats.{InMemoryStatsReceiver, StatsReceiver}
import com.twitter.inject.TwitterModule

object InMemoryStatsReceiverModule extends TwitterModule {
  override def configure(): Unit = {
    bindSingleton[StatsReceiver].to[InMemoryStatsReceiver]
  }
}
