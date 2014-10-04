package com.twitter.finatra.integration

import com.google.inject.AbstractModule
import com.twitter.finagle.NoStacktrace
import com.twitter.finagle.http.Status
import com.twitter.finatra.FinatraServer
import com.twitter.finatra.conversions.time._
import com.twitter.finatra.guice.GuiceModule
import com.twitter.finatra.test.{EmbeddedTwitterServer, HttpTest}
import com.twitter.finatra.twitterserver.routing.Router
import com.twitter.finatra.twitterserver.{GuiceTwitterServer, TwitterServerWithPorts}
import com.twitter.server.Lifecycle.Warmup


class StartupIntegrationTest extends HttpTest {

  "startup" should {
    "ensure health check succeeds when guice config is good" in {
      val server = EmbeddedTwitterServer(SimpleGuiceHttpTwitterServer)
      assertHealth(server)

      //We can no longer directly get the json, since it's now embedded in HTML :-/
      server.httpGet(
        "/admin/server_info",
        andExpect = Status.Ok)

      server.close()
    }

    "non HTTP twitter-server passes health check" in {
      val server = EmbeddedTwitterServer(
        SimpleGuiceTwitterServer)

      server.start()
      assertHealth(server)
      server.close()
    }

    "ensure server health check fails when guice config fails fast" in {
      val server = EmbeddedTwitterServer(
        twitterServer = FailFastServer,
        waitForWarmup = false)

      server.start()
      server.guiceApp.postWarmupComplete should equal(false)
      assertFailedFuture[StartupTestException](server.mainResult)
    }

    "ensure startup fails when guice config hangs" in {
      val server = EmbeddedTwitterServer(
        twitterServer = GuiceStartupHangsServer,
        waitForWarmup = false)

      server.start()
      Thread.sleep(2000) //since the guice module hangs forever, we simply make sure we aren't healthy after 2 seconds...
      assertHealth(server, healthy = false)
      server.close()
    }

    "ensure startup fails when simple server preMain throws exception" in {
      val server = new EmbeddedTwitterServer(
        twitterServer = PremainExceptionServer,
        waitForWarmup = false)

      assertFailedFuture[StartupTestException](
        server.mainResult)
    }

    "ensure startup fails when preMain throws exception" in {
      val server = new EmbeddedTwitterServer(
        twitterServer = ServerPremainException,
        waitForWarmup = false)

      assertFailedFuture[StartupTestException](
        server.mainResult)
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

      assertHealth(server, healthy = false)
      sleep(3.seconds, verbose = true)
      assertHealth(server, healthy = false)
      continueWarmup = false
      sleep(3.seconds, verbose = true)
      assertHealth(server)
    }

    "calling GuiceModule.install throws exception" in {
      val server = new EmbeddedTwitterServer(
        twitterServer = ServerWithGuiceModuleInstall,
        waitForWarmup = false)

      assertFailedFuture[Exception](
        server.mainResult)
    }
  }
}

object GuiceStartupHangsServer extends FinatraServer {
  override def overrideModules = Seq(new AbstractModule {
    def configure() {
      Thread.sleep(99999999)
    }
  })

  protected override def configure(router: Router) {}
}

object FailFastServer extends FinatraServer {
  override val modules = Seq(new AbstractModule {
    def configure() {
      throw new StartupTestException("guice module exception")
    }
  })

  protected override def configure(router: Router) {}
}

object SimpleGuiceTwitterServer extends GuiceTwitterServer {
  override val modules = Seq()
}

object SimpleGuiceHttpTwitterServer extends FinatraServer {
  protected override def configure(router: Router) {}
}

object ServerWithGuiceModuleInstall extends FinatraServer {
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

object PremainExceptionServer extends TwitterServerWithPorts with Warmup {
  premain {
    throw new StartupTestException("premain exception")
  }

  def main() {
    warmupComplete()
    throw new StartupTestException("shouldn't get here")
  }
}

object ServerPremainException extends FinatraServer {
  premain {
    throw new StartupTestException("premain exception")
  }

  override def configure(router: Router) {}
}

class StartupTestException(msg: String) extends Exception(msg) with NoStacktrace
