package com.twitter.finatra.integration

import com.twitter.finagle.http.Status
import com.twitter.finatra.test.{EmbeddedTwitterServer, Test}
import com.twitter.finatra.twitterserver.GuiceTwitterServer
import com.twitter.util.Future

class EmbeddedTwitterServerIntegrationTest extends Test {

  "server" should {
    "start" in {
      val server = EmbeddedTwitterServer(new GuiceTwitterServer {})

      server.httpGet(
        "/health",
        routeToAdminServer = true,
        andExpect = Status.Ok,
        withBody = "OK\n")

      server.close()
    }
    
    "fail if server is a singleton" in {
      intercept[IllegalArgumentException] {
        EmbeddedTwitterServer(SingletonServer)
      }
    }
  }
}

object SingletonServer extends GuiceTwitterServer
