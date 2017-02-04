package com.twitter.inject.modules.tests

import com.google.inject.Guice
import com.twitter.finagle.stats.StatsReceiver
import com.twitter.inject.WordSpecTest
import com.twitter.inject.modules.{InMemoryStatsReceiverModule, StatsReceiverModule}

class StatsReceiverModuleTest extends WordSpecTest {

  "StatsReceiverModule" in {
    val injector = Guice.createInjector(StatsReceiverModule)
    injector.getInstance(classOf[StatsReceiver])
  }

  "InMemoryStatsReceiverModule" in {
    val injector = Guice.createInjector(InMemoryStatsReceiverModule)
    injector.getInstance(classOf[StatsReceiver])
  }

}
