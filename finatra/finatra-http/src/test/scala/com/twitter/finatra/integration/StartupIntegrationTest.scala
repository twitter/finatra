package com.twitter.finatra.integration

import com.google.inject.AbstractModule
import com.twitter.finagle.http.Status
import com.twitter.finatra.FinatraServer
import com.twitter.finatra.conversions.time._
import com.twitter.finatra.guice.GuiceModule
import com.twitter.finatra.routing.Router
import com.twitter.finatra.test.{EmbeddedTwitterServer, HttpTest}
import com.twitter.finatra.twitterserver.{GuiceTwitterServer, TwitterServerWithPorts}
import com.twitter.server.Lifecycle.Warmup
import com.twitter.util.Await


class StartupIntegrationTest extends HttpTest {

  "startup" should {
    "ensure health check succeeds when guice config is good" in {
      val server = EmbeddedTwitterServer(new SimpleGuiceHttpTwitterServer)
      server.assertHealthy()

      //We can no longer directly get the json, since it's now embedded in HTML
      server.httpGet(
        "/admin/server_info",
        andExpect = Status.Ok)

      server.close()
    }

    "non HTTP twitter-server passes health check" in {
      val server = EmbeddedTwitterServer(
        new SimpleGuiceTwitterServer)

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
        twitterServer = new GuiceTwitterServer {})

      server.assertHealthy()
      server.close()
    }

    "ensure server health check fails when guice config fails fast" in {
      val server = EmbeddedTwitterServer(
        twitterServer = new FailFastServer,
        waitForWarmup = false)

      server.start()
      server.guiceApp.postWarmupComplete should equal(false)

      //TODO: Remove comment once we switch to app.nonExitingMain:
      //assertFailedFuture[StartupTestException](server.mainResult)
      server.close()
    }

    "ensure startup fails when guice config hangs" in {
      val server = EmbeddedTwitterServer(
        twitterServer = new GuiceStartupHangsServer,
        waitForWarmup = false)

      server.start()
      Thread.sleep(2000) //since the guice module hangs forever, we simply make sure we aren't healthy after 2 seconds...
      server.assertHealthy(healthy = false)
      server.close()
    }

    "ensure startup fails when simple server preMain throws exception" in {
      val server = new EmbeddedTwitterServer(
        twitterServer = new PremainExceptionServer,
        waitForWarmup = false)

      server.start()

      //TODO: Remove comment once we switch to app.nonExitingMain:
      //assertFailedFuture[StartupTestException](server.mainResult)
      server.close()
    }

    "ensure startup fails when preMain throws exception" in {
      val server = new EmbeddedTwitterServer(
        twitterServer = new ServerPremainException,
        waitForWarmup = false)

      server.start()
      server.guiceApp.postWarmupComplete should equal(false)

      //TODO: Remove comment once we switch to app.nonExitingMain:
      //assertFailedFuture[StartupTestException](server.mainResult)
      server.close()
    }

    "ensure http server starts after warmup" in {
      pending //don't normally run since requires sleeps :-/
      var continueWarmup = true

      class WarmupServer extends FinatraServer {

        override def warmup {
          println("Warmup begin")
          while (continueWarmup) {
            Thread.sleep(1000)
          }
          println("Warmup end")
        }

        override def configure(router: Router) {}
      }

      val server = new EmbeddedTwitterServer(
        twitterServer = new WarmupServer,
        waitForWarmup = false)

      server.assertHealthy(healthy = false)
      sleep(3.seconds, verbose = true)

      server.assertHealthy(healthy = false)
      continueWarmup = false
      sleep(3.seconds, verbose = true)

      server.assertHealthy(healthy = true)
      server.close()
    }

    "calling GuiceModule.install throws exception" in {
      val server = new EmbeddedTwitterServer(
        twitterServer = new ServerWithGuiceModuleInstall,
        waitForWarmup = false)

      assertFailedFuture[Throwable](server.mainResult)
      server.close()
    }
  }
}

class GuiceStartupHangsServer extends FinatraServer {
  override def overrideModules = Seq(new AbstractModule {
    def configure() {
      Thread.sleep(99999999)
    }
  })

  protected override def configure(router: Router) {}
}

class FailFastServer extends FinatraServer {
  override val modules = Seq(new AbstractModule {
    def configure() {
      throw new StartupTestException("guice module exception")
    }
  })

  protected override def configure(router: Router) {}
}

class SimpleGuiceTwitterServer extends GuiceTwitterServer {
  override val modules = Seq()
}

class SimpleGuiceHttpTwitterServer extends FinatraServer {
  protected override def configure(router: Router) {}
}

class ServerWithGuiceModuleInstall extends FinatraServer {
  override val modules = Seq(new GuiceModule {
    override def configure() {
      install(new FooModule)
    }
  })

  protected override def configure(router: Router) {}
}

class FooModule extends AbstractModule {
  override def configure() {}
}

class PremainExceptionServer extends TwitterServerWithPorts with Warmup {
  premain {
    throw new StartupTestException("premain exception")
  }

  /* TODO: Remove once com.twitter.app.App with nonExitingMain is opensource released */
  override protected def exitOnError(reason: String): Unit = {
    log.warning(reason)
  }

  def main() {
    warmupComplete()
    throw new StartupTestException("shouldn't get here")
  }
}

class ServerPremainException extends FinatraServer {
  premain {
    throw new StartupTestException("premain exception")
  }

  override def configure(router: Router) {}
}

class StartupTestException(msg: String) extends Exception(msg)

class RawTwitterServerWithPorts extends TwitterServerWithPorts {
  def main() {
    Await.ready(
      adminHttpServer)
  }
}