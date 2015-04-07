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

      //We can no longer directly get the json, since it's now embedded in HTML
      server.httpGetAdmin(
        "/admin/server_info",
        andExpect = Status.Ok)

      server.close()
    }

    "non HTTP twitter-server passes health check" in {
      val server = new EmbeddedTwitterServer(
        new SimpleGuiceTwitterServer)

      server.assertHealthy()
      server.close()
    }

    "raw TwitterServerWithPorts starts up" in {
      val server =new EmbeddedTwitterServer(
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
      val server = new EmbeddedTwitterServer(
        twitterServer = new FailFastServer,
        waitForWarmup = false)

      server.start()
      server.guiceApp.appStarted should equal(false)

      //TODO: Remove comment once we switch to app.nonExitingMain:
      //assertFailedFuture[StartupTestException](server.mainResult)
      server.close()
    }

    "ensure startup fails when guice config hangs" in {
      val server = new EmbeddedTwitterServer(
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
      server.guiceApp.appStarted should equal(false)

      //TODO: Remove comment once we switch to app.nonExitingMain:
      //assertFailedFuture[StartupTestException](server.mainResult)
      server.close()
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
      val server = new EmbeddedTwitterServer(
        twitterServer = new ServerWithGuiceModuleInstall,
        waitForWarmup = false)

      assertFailedFuture[Throwable](server.mainResult)
      server.close()
    }
  }
}

class GuiceStartupHangsServer extends TwitterServer {
  override def overrideModules = Seq(new AbstractModule {
    def configure() {
      Thread.sleep(99999999)
    }
  })
}

class FailFastServer extends TwitterServer {
  override val modules = Seq(new AbstractModule {
    def configure() {
      throw new StartupTestException("guice module exception")
    }
  })
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
}

class FooModule extends AbstractModule {
  override def configure() {}
}

class PremainExceptionServer extends BaseTwitterServer with Ports with Warmup {
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

class ServerPremainException extends TwitterServer {
  premain {
    throw new StartupTestException("premain exception")
  }
}

class StartupTestException(msg: String) extends Exception(msg)

class RawTwitterServerWithPorts extends BaseTwitterServer with Ports {
  def main() {
    Await.ready(
      adminHttpServer)
  }
}