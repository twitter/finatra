package com.twitter.finatra.http.modules

import com.google.inject.Provides
import com.twitter.finagle.http.Request
import com.twitter.finagle.http.filter.StatsFilter
import com.twitter.finagle.stats.StatsReceiver
import com.twitter.inject.TwitterModule
import javax.inject.Singleton

object StatsFilterModule extends TwitterModule {

  @Provides
  @Singleton
  def provideStatsFilter(statsReceiver: StatsReceiver): StatsFilter[Request] = {
    new StatsFilter[Request](statsReceiver)
  }
}
