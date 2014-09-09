package com.twitter.finatra.guice

import com.twitter.finagle.stats.{InMemoryStatsReceiver, StatsReceiver}

object StatsInMemoryModule extends GuiceModule {
  override def configure() {
    bindSingleton[StatsReceiver].to[InMemoryStatsReceiver]
  }
}
