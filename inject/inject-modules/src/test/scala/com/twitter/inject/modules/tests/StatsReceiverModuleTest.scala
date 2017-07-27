package com.twitter.inject.modules.tests

import com.google.inject.Guice
import com.twitter.finagle.stats.StatsReceiver
import com.twitter.inject.Test
import com.twitter.inject.modules.{InMemoryStatsReceiverModule, StatsReceiverModule}

class StatsReceiverModuleTest extends Test {

  test("StatsReceiverModule") {
    val injector = Guice.createInjector(StatsReceiverModule)
    injector.getInstance(classOf[StatsReceiver])
  }

  test("InMemoryStatsReceiverModule") {
    val injector = Guice.createInjector(InMemoryStatsReceiverModule)
    injector.getInstance(classOf[StatsReceiver])
  }

}
