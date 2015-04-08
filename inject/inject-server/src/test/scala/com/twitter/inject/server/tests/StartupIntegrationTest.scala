package com.twitter.inject.server.tests

import com.google.inject.AbstractModule
import com.twitter.finagle.http.Status
import com.twitter.inject.server.{EmbeddedTwitterServer, Ports, TwitterServer}
import com.twitter.inject.{Test, TwitterModule}
import com.twitter.server.Lifecycle.Warmup
import com.twitter.server.{TwitterServer => BaseTwitterServer}
import com.twitter.util.Await


class StartupIntegrationTest extends Test {

  "startup" should {
    "ensure health check succeeds when guice config is good" in {
      val server = new EmbeddedTwitterServer(new SimpleGuiceHttpTwitterServer)
      server.assertHealthy()

      server.httpGetAdmin(
        "/admin/server_info",
        andExpect = Status.Ok)

      server.close()
    }

    "non HTTP twitter-server passes health check" in {
      val server = new EmbeddedTwitterServer(new SimpleGuiceTwitterServer)
      server.assertHealthy()
      server.close()
    }

    "raw TwitterServerWithPorts starts up" in {
      val server = new EmbeddedTwitterServer(
        twitterServer = new RawTwitterServerWithPorts)

      server.assertHealthy()
      server.close()
    }

    "GuiceTwitterServer starts up" in {
      val server = new EmbeddedTwitterServer(
        twitterServer = new TwitterServer {})

      server.assertHealthy()
      server.close()
    }

    "ensure server health check fails when guice config fails fast" in {
      intercept[Exception] {
        new EmbeddedTwitterServer(
          new FailFastServer).start()
      }
    }

    "ensure startup fails when base twitter server preMain throws exception" in {
      intercept[Exception] {
        new EmbeddedTwitterServer(
          twitterServer = new PremainErrorBaseTwitterServer).start()
      }
    }

    "ensure startup fails when preMain throws exception" in {
      intercept[Exception] {
        new EmbeddedTwitterServer(
          new ServerPremainException).start()
      }
    }

    "ensure http server starts after warmup" in {
      pending //only manually run since uses sleeps
      var continueWarmup = true

      class WarmupServer extends TwitterServer {

        override def warmup {
          println("Warmup begin")
          while (continueWarmup) {
            Thread.sleep(1000)
          }
          println("Warmup end")
        }
      }

      val server = new EmbeddedTwitterServer(
        twitterServer = new WarmupServer,
        waitForWarmup = false)

      server.assertHealthy(healthy = false)
      Thread.sleep(3000)

      server.assertHealthy(healthy = false)
      continueWarmup = false
      Thread.sleep(3000)

      server.assertHealthy(healthy = true)
      server.close()
    }

    "calling GuiceModule.install throws exception" in {
      intercept[Exception] {
        new EmbeddedTwitterServer(
          twitterServer = new ServerWithGuiceModuleInstall).start()
      }
    }
  }
}

class FailFastServer extends TwitterServer {
  override val modules = Seq(new AbstractModule {
    def configure() {
      throw new StartupTestException("guice module exception")
    }
  })

  //TODO: Remove override once App#nonExitingMain in opensource released
  override protected def exitOnError(reason: String) {
    System.err.println(reason)
    close()
    throw new Exception(reason)
  }
}

class SimpleGuiceTwitterServer extends TwitterServer {
  override val modules = Seq()
}

class SimpleGuiceHttpTwitterServer extends TwitterServer {
}

class ServerWithGuiceModuleInstall extends TwitterServer {
  override val modules = Seq(new TwitterModule {
    override def configure() {
      install(new FooModule)
    }
  })

  //TODO: Remove override once App#nonExitingMain in opensource released
  override protected def exitOnError(reason: String) {
    System.err.println(reason)
    close()
    throw new Exception(reason)
  }
}

class FooModule extends AbstractModule {
  override def configure() {}
}

class PremainErrorBaseTwitterServer extends BaseTwitterServer with Ports with Warmup {
  premain {
    throw new StartupTestException("premain exception")
  }

  /* TODO: Remove once com.twitter.app.App with nonExitingMain is opensource released */
  override protected def exitOnError(reason: String): Unit = {
    System.err.println(reason)
    close()
    throw new Exception(reason)
  }

  def main() {
    warmupComplete()
    throw new StartupTestException("shouldn't get here")
  }
}

class ServerPremainException extends TwitterServer {
  premain {
    throw new StartupTestException("premain exception")
  }

  //TODO: Remove override once App#nonExitingMain in opensource released
  override protected def exitOnError(reason: String) {
    System.err.println(reason)
    close()
    throw new Exception(reason)
  }
}

class StartupTestException(msg: String) extends Exception(msg)

class RawTwitterServerWithPorts extends BaseTwitterServer with Ports {
  def main() {
    Await.ready(
      adminHttpServer)
  }
}