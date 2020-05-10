package com.twitter.inject.server.tests

import com.google.inject.{AbstractModule, Module}
import com.twitter.app.CloseException
import com.twitter.conversions.DurationOps._
import com.twitter.finagle.http.Status
import com.twitter.inject.app.App
import com.twitter.inject.server.{EmbeddedTwitterServer, Ports, TwitterServer}
import com.twitter.inject.{Injector, Test, TwitterModule}
import com.twitter.server.Lifecycle.Warmup
import com.twitter.server.{TwitterServer => BaseTwitterServer}
import com.twitter.util.{Await, Closable, Duration, Future}
import com.twitter.util.registry.GlobalRegistry
import scala.util.parsing.json.JSON

class StartupIntegrationTest extends Test {

  override protected def afterEach(): Unit = {
    // "clear" GlobalRegistry
    GlobalRegistry.get.iterator foreach { entry =>
      GlobalRegistry.get.remove(entry.key)
    }
    super.afterEach()
  }

  test("ensure health check succeeds when guice config is good") {
    val server = new EmbeddedTwitterServer(new SimpleHttpTwitterServer, disableTestLogging = true)
    run(server) {
      server.assertHealthy()
      server.httpGetAdmin("/admin/server_info", andExpect = Status.Ok)
    }
  }

  test("non HTTP twitter-server passes health check") {
    val server = new EmbeddedTwitterServer(new SimpleTwitterServer, disableTestLogging = true)
    run(server) {
      server.assertHealthy()
    }
  }

  test("embedded raw com.twitter.server.Twitter starts up") {
    val server = new EmbeddedTwitterServer(
      twitterServer = new ExtendedBaseTwitterServer,
      disableTestLogging = true)
    run(server) {
      server.assertHealthy()
    }
  }

  test("TwitterServer starts up") {
    val server =
      new EmbeddedTwitterServer(twitterServer = new TwitterServer {}, disableTestLogging = true)
    run(server) {
      server.assertHealthy()
    }
  }

  test("ensure server health check fails when guice config fails fast") {
    val server = new EmbeddedTwitterServer(new FailFastServer, disableTestLogging = true)
    run(server) {
      intercept[Exception] {
        server.start()
      }
    }
  }

  test("ensure startup fails when base twitter server preMain throws exception") {
    val server =
      new EmbeddedTwitterServer(new PremainErrorBaseTwitterServer, disableTestLogging = true)
    run(server) {
      intercept[Exception] {
        server.start()
      }
    }
  }

  test("ensure startup fails when preMain throws exception") {
    val server = new EmbeddedTwitterServer(new ServerPremainException, disableTestLogging = true)
    run(server) {
      intercept[Exception] {
        server.start()
      }
    }
  }

  test("ensure http server starts after warmup") {
    class WarmupServer extends TwitterServer {

      override def warmup(): Unit = {
        info("Warmup begin")
        for (_ <- 1 to 10) { info("Warming up...") }
        info("Warmup end")
      }
    }

    val server =
      new EmbeddedTwitterServer(twitterServer = new WarmupServer, disableTestLogging = true)
    run(server) {
      server.assertHealthy()
    }
  }

  test("calling install without a TwitterModule works") {
    val server = new EmbeddedTwitterServer(new ServerWithModuleInstall, disableTestLogging = true)
    run(server) {
      server.start()
    }
  }

  test("calling install with a TwitterModule throws exception") {
    val server =
      new EmbeddedTwitterServer(new ServerWithTwitterModuleInstall, disableTestLogging = true)
    run(server) {
      intercept[Exception] {
        server.start()
      }
    }
  }

  test("injector called before main") {
    val app: App = new App {
      override val modules: Seq[Module] = Seq(new TwitterModule {})
    }
    val e = intercept[Exception] {
      app.injector
    }
    app.close()
    e.getMessage should include("injector is not available before main")
  }

  test("register framework library") {
    val server = new EmbeddedTwitterServer(new ServerWithModuleInstall, disableTestLogging = true)
    try {
      server.start()

      val response = server.httpGetAdmin("/admin/registry.json", andExpect = Status.Ok)

      val json: Map[String, Any] =
        JSON.parseFull(response.contentString).get.asInstanceOf[Map[String, Any]]
      val registry = json("registry").asInstanceOf[Map[String, Any]]
      assert(registry.contains("library"))
      assert(registry("library").asInstanceOf[Map[String, String]].contains("finatra"))
    } finally {
      server.close()
    }
  }

  test("server error on shutdown") {
    val underlying = new ErrorOnExitTwitterServer
    // ErrorOnExitTwitterServer throws 5 non-fatal errors and 1 fatal error on shutdown,
    // the fatal is in the closeOnExitLast, so we see all the errors
    val server = new EmbeddedTwitterServer(underlying, disableTestLogging = true)

    try {
      server.assertHealthy()
    } finally {
      server.close()
    }

    val e = intercept[CloseException] {
      server.assertCleanShutdown()
    }
    e.getSuppressed.length should equal(7)

    // a non-fatal during a module closing down shouldn't interrupt others
    underlying.allModulesShutdown should be(true)
  }

  test("fatal server errors on shutdown") {
    // FatalErrorOnExitTwitterServer throws 3 fatal errors on shutdown
    val server =
      new EmbeddedTwitterServer(new FatalErrorOnExitTwitterServer, disableTestLogging = true)

    server.assertHealthy()

    intercept[InterruptedException] { // the first fatal exception escapes.
      server.close()
    }
    server
      .assertCleanShutdown() // only captures NonFatal exceptions as Fatals already fail closing.
  }

  test("fatal base server error on shutdown") {
    // FatalOnExitBaseTwitterServer throws 1 fatal on shutdown
    val server =
      new EmbeddedTwitterServer(new FatalOnExitBaseTwitterServer, disableTestLogging = true)

    server.assertHealthy()
    server.close()
    intercept[CloseException] { // the fatal is getting wrapped in a java.util.concurrent.ExecutionException
      server.assertCleanShutdown()
    }
  }

  private[this] def run(server: EmbeddedTwitterServer)(f: => Unit): Unit = {
    try {
      f
    } finally {
      close(server)
    }
  }

  private[this] def close(server: EmbeddedTwitterServer): Unit = {
    server.close()
    server.assertCleanShutdown()
  }
}

class FailFastServer extends TwitterServer {
  override val modules: Seq[Module] = Seq(new AbstractModule {
    override def configure(): Unit = {
      throw new StartupTestException("guice module exception")
    }
  })
}

class SimpleTwitterServer extends TwitterServer {
  /* ensure enough time to close resources */
  override val defaultCloseGracePeriod: Duration = 15.seconds
  override val modules: Seq[Module] = Seq.empty[Module]
}

class SimpleHttpTwitterServer extends TwitterServer {}

class ServerWithTwitterModuleInstall extends TwitterServer {
  /* ensure enough time to close resources */
  override val defaultCloseGracePeriod: Duration = 15.seconds
  override val modules: Seq[Module] = Seq(new TwitterModule {
    override def configure(): Unit = {
      install(new TwitterModule {})
    }
  })
}

class ServerWithModuleInstall extends TwitterServer {
  /* ensure enough time to close resources */
  override val defaultCloseGracePeriod: Duration = 15.seconds
  override val modules: Seq[Module] = Seq(new TwitterModule {
    override def configure(): Unit = {
      install(new AbstractModule {
        override def configure(): Unit = {}
      })
    }
  })
}

class PremainErrorBaseTwitterServer extends BaseTwitterServer with Ports with Warmup {
  premain {
    throw new StartupTestException("premain exception")
  }

  def main(): Unit = {
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

class ExtendedBaseTwitterServer extends BaseTwitterServer {
  /* ensure enough time to close resources */
  override val defaultCloseGracePeriod: Duration = 15.seconds
  def main(): Unit = {
    Await.ready(adminHttpServer)
  }
}

class ErrorOnExitTwitterServer extends TwitterServer {
  var moduleAShutdown, moduleBShutdown = false

  override val modules: Seq[Module] = Seq(
    new TwitterModule {
      override protected[inject] def singletonShutdown(injector: Injector): Unit = {
        moduleAShutdown = true
        throw new Exception("FORCED EXCEPTION in SingletonShutdown")
      }
    },
    new TwitterModule {
      override protected[inject] def singletonShutdown(injector: Injector): Unit = {
        moduleBShutdown = true
        throw new Exception("FORCED EXCEPTION in SingletonShutdown")
      }
    }
  )

  def allModulesShutdown: Boolean = moduleAShutdown && moduleBShutdown

  override protected def postWarmup(): Unit = {
    onExit {
      throw new IllegalStateException("FORCED IllegalStateException ON SHUTDOWN")
    }

    onExit {
      throw new IllegalArgumentException("FORCED IllegalArgumentException ON SHUTDOWN")
    }

    onExit {
      throw new NullPointerException("FORCED NullPointerException ON SHUTDOWN")
    }

    closeOnExit {
      Closable.make { _ =>
        Future.exception(new Exception("FORCED Exception IN CLOSE ON EXIT"))
      }
    }

    closeOnExitLast {
      Closable.make { _ =>
        Future.exception(throw new Exception("FORCED thrown Exception IN CLOSE ON EXIT LAST"))
      }
    }
  }
}

class FatalErrorOnExitTwitterServer extends TwitterServer {
  override val modules: Seq[Module] = Seq(
    new TwitterModule {
      override protected[inject] def singletonShutdown(injector: Injector): Unit =
        throw new InterruptedException("FORCED FATAL EXCEPTION in SingletonShutdown")
    },
    new TwitterModule {
      override protected[inject] def singletonShutdown(injector: Injector): Unit =
        throw new InterruptedException("FORCED FATAL EXCEPTION in SingletonShutdown")
    }
  )

  override protected def postWarmup(): Unit = {
    onExit {
      throw new InterruptedException("FORCED FATAL EXCEPTION in postWarmup")
    }

    closeOnExit {
      Closable.make { _ =>
        throw new InterruptedException("FORCED FATAL EXCEPTION IN CLOSE ON EXIT")
      }
    }

    closeOnExit {
      Closable.make { _ =>
        Future.exception(new Exception("FORCED EXCEPTION IN CLOSE ON EXIT"))
      }
    }
  }
}

class FatalOnExitBaseTwitterServer extends BaseTwitterServer with Ports with Warmup {
  def main(): Unit = {
    onExit {
      throw new InterruptedException("FORCED FATAL EXCEPTION in main")
    }
    warmupComplete()
    Await.ready(adminHttpServer)
  }
}
