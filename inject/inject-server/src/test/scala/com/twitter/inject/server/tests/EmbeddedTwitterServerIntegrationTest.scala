package com.twitter.inject.server.tests

import com.twitter.finagle.http.Status
import com.twitter.inject.server.{EmbeddedTwitterServer, TwitterServer}
import com.twitter.inject.{Test, TwitterModule}

class EmbeddedTwitterServerIntegrationTest extends Test {

  "server" should {
    "start" in {
      val twitterServer = new TwitterServer {}
      twitterServer.addFrameworkOverrideModules(new TwitterModule {})
      val embeddedServer = new EmbeddedTwitterServer(twitterServer)

      embeddedServer.httpGetAdmin(
        "/health",
        andExpect = Status.Ok,
        withBody = "OK\n")

      embeddedServer.close()
    }

    "fail if server is a singleton" in {
      intercept[IllegalArgumentException] {
        new EmbeddedTwitterServer(SingletonServer)
      }
    }
  }
}

object SingletonServer extends TwitterServer
