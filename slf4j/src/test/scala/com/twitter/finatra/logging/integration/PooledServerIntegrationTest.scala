package com.twitter.finatra.logging.integration

import com.twitter.finagle.httpx.Status._
import com.twitter.finatra.http.test.EmbeddedHttpServer
import com.twitter.inject.Test
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
