package com.twitter.inject.modules

import com.twitter.finagle.stats.{LoadedStatsReceiver, StatsReceiver}
import com.twitter.inject.TwitterModule

object StatsReceiverModule extends TwitterModule {
  override def configure(): Unit = {
    bind[StatsReceiver].toInstance(LoadedStatsReceiver)
  }

  /**  Java-friendly way to access this module as a singleton instance */
  def apply(): this.type = this
}
