package com.twitter.finatra.integration

import com.twitter.finagle.http.Status
import com.twitter.finatra.test.{EmbeddedTwitterServer, Test}
import com.twitter.finatra.twitterserver.GuiceTwitterServer
import com.twitter.util.Future

class EmbeddedTwitterServerIntegrationTest extends Test {

  "server" should {
    "start and quit" in {
      val server = EmbeddedTwitterServer(new GuiceTwitterServer {})

      server.httpPost(
        "/quitquitquit",
        postBody = "",
        routeToAdminServer = true,
        andExpect = Status.Ok,
        withBody = "quitting\n")

      assertFuture(
        server.mainResult,
        Future.Unit)
    }
  }
}
