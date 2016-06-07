package com.twitter.finatra.http.tests.integration.pools.test

import com.twitter.finagle.http.Status._
import com.twitter.finatra.http.tests.integration.pools.main.{PooledController, PooledServer}
import com.twitter.finatra.http.EmbeddedHttpServer
import com.twitter.inject.server.FeatureTest

class PooledServerIntegrationTest extends FeatureTest {

  override val server = new EmbeddedHttpServer(new PooledServer)

  "PooledServer should return right away" in {
    for (i <- 1 to 100) {
      server.httpGet("/hi?id=" + i, andExpect = Ok)
    }

    //When manually testing, uncomment the next line to wait for forked future logging
    //Thread.sleep(10000)

    PooledController.pool1.executor.shutdownNow()
    PooledController.pool2.executor.shutdownNow()
  }
}
