package com.twitter.inject.server.tests

import com.twitter.finagle.http.Status
import com.twitter.inject.server.{EmbeddedTwitterServer, TwitterServer}
import com.twitter.inject.{Logging, Test, TwitterModule}

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

    "fail if bind on a non-injectable server" in {
      intercept[IllegalStateException] {
        new EmbeddedTwitterServer(
          new NonInjectableServer)
          .bind[String]("hello!")
      }
    }

    "support bind in server" in {
      val server = new EmbeddedTwitterServer(
        new TwitterServer {})
        .bind[String]("helloworld")

      server.injector.instance[String] should be("helloworld")
      server.close()
    }

    "fail because of unknown flag" in {
      val server = new EmbeddedTwitterServer(
        new TwitterServer {},
        flags = Map("foo.bar" -> "true"))

      val e = intercept[Exception] {
        server.assertHealthy()
      }
      e.getMessage.contains("Error parsing flag \"foo.bar\": flag undefined") should be(true)
      server.close()
    }
  }
}

class NonInjectableServer
  extends com.twitter.server.TwitterServer
  with Logging {
  def main(): Unit = {
    info("Hello World")
  }
}

object SingletonServer extends TwitterServer
