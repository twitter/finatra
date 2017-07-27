package com.twitter.inject.server.tests

import com.twitter.finagle.http.Status
import com.twitter.inject.server.{EmbeddedTwitterServer, TwitterServer}
import com.twitter.inject.{Logging, Test, TwitterModule}

class EmbeddedTwitterServerIntegrationTest extends Test {

  test("server#start") {
    val twitterServer = new TwitterServer {}
    twitterServer.addFrameworkOverrideModules(new TwitterModule {})
    val embeddedServer = new EmbeddedTwitterServer(twitterServer)

    embeddedServer.httpGetAdmin("/health", andExpect = Status.Ok, withBody = "OK\n")

    embeddedServer.close()
  }

  test("server#fail if server is a singleton") {
    intercept[IllegalArgumentException] {
      new EmbeddedTwitterServer(SingletonServer)
    }
  }

  test("server#fail if bind on a non-injectable server") {
    intercept[IllegalStateException] {
      new EmbeddedTwitterServer(new NonInjectableServer)
        .bind[String]("hello!")
    }
  }

  test("server#support bind in server") {
    val server = new EmbeddedTwitterServer(new TwitterServer {})
      .bind[String]("helloworld")

    server.injector.instance[String] should be("helloworld")
    server.close()
  }

  test("server#fail because of unknown flag") {
    val server = new EmbeddedTwitterServer(new TwitterServer {}, flags = Map("foo.bar" -> "true"))

    val e = intercept[Exception] {
      server.assertHealthy()
    }
    e.getMessage.contains("Error parsing flag \"foo.bar\": flag undefined") should be(true)
    server.close()
  }
}

class NonInjectableServer extends com.twitter.server.TwitterServer with Logging {
  def main(): Unit = {
    info("Hello World")
  }
}

object SingletonServer extends TwitterServer
