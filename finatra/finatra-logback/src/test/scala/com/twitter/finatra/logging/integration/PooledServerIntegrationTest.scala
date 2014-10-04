package com.twitter.finatra.logging.integration

import com.twitter.finagle.http.Status._
import com.twitter.finatra.test.{EmbeddedTwitterServer, HttpTest}

class PooledServerIntegrationTest extends HttpTest {

  "PooledServer" should {
    val server = new EmbeddedTwitterServer(
      twitterServer = PooledServerMain)

    "return right away" in {
      for (i <- 1 to 100) {
        server.httpGet("/hi?id=" + i, andExpect = Ok)
      }

      //When manually testing, uncomment the next line to wait for forked future logging
      //Thread.sleep(10000)
    }
  }
}
