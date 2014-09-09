package com.twitter.finatra.modules

import com.twitter.finagle.stats.{LoadedStatsReceiver, StatsReceiver}
import com.twitter.finatra.guice.GuiceModule

object LoadedStatsModule extends GuiceModule {
  override def configure() {
    bindSingleton[StatsReceiver].toInstance(LoadedStatsReceiver)
  }
}
