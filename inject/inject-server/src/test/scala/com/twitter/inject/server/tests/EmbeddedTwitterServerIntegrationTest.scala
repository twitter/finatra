package com.twitter.inject.server.tests

import com.twitter.finagle.http.Status
import com.twitter.inject.Test
import com.twitter.inject.server.{EmbeddedTwitterServer, TwitterServer}

class EmbeddedTwitterServerIntegrationTest extends Test {

  "server" should {
    "start" in {
      val server = new EmbeddedTwitterServer(new TwitterServer {})

      server.httpGetAdmin(
        "/health",
        andExpect = Status.Ok,
        withBody = "OK\n")

      server.close()
    }
    
    "fail if server is a singleton" in {
      intercept[IllegalArgumentException] {
        new EmbeddedTwitterServer(SingletonServer)
      }
    }
  }
}

object SingletonServer extends TwitterServer
