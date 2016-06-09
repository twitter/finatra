package com.twitter.finatra.http.tests.integration.doeverything.main.modules

import com.twitter.finagle.stats.{LoadedStatsReceiver, StatsReceiver}
import com.twitter.inject.TwitterModule

object DoEverythingStatsReceiverModule extends TwitterModule {
  override def configure() {
    bindSingleton[StatsReceiver].toInstance(LoadedStatsReceiver.scope("do_everything"))
  }
}
