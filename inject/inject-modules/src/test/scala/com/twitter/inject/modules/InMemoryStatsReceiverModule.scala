package com.twitter.inject.modules

import com.twitter.finagle.stats.{InMemoryStatsReceiver, StatsReceiver}
import com.twitter.inject.TwitterModule
import javax.inject.Singleton

object InMemoryStatsReceiverModule extends TwitterModule {
  override def configure(): Unit = {
    bind[StatsReceiver].to[InMemoryStatsReceiver].in[Singleton]
  }

  /**  Java-friendly way to access this module as a singleton instance */
  def get(): this.type = this
}
