package com.twitter.finatra.logging.integration

import com.twitter.finagle.http.Status._
import com.twitter.finatra.http.test.EmbeddedHttpServer
import com.twitter.inject.Test

class PooledServerIntegrationTest extends Test {

  "PooledServer" should {
    val server = new EmbeddedHttpServer(
      twitterServer = new PooledServer)

    "return right away" in {
      for (i <- 1 to 100) {
        server.httpGet("/hi?id=" + i, andExpect = Ok)
      }

      //When manually testing, uncomment the next line to wait for forked future logging
      //Thread.sleep(10000)
    }
  }
}
