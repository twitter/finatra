package com.twitter.inject.modules

import com.twitter.finagle.stats.{LoadedStatsReceiver, StatsReceiver}
import com.twitter.inject.TwitterModule

object StatsReceiverModule extends TwitterModule {
  override def configure() {
    bindSingleton[StatsReceiver].toInstance(LoadedStatsReceiver)
  }
}
