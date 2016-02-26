package com.twitter.inject.server

import com.google.inject.Module
import com.twitter.finagle.client.ClientRegistry
import com.twitter.finagle.http.HttpMuxer
import com.twitter.finatra.logging.modules.Slf4jBridgeModule
import com.twitter.inject.Logging
import com.twitter.inject.app.App
import com.twitter.inject.modules.StatsReceiverModule
import com.twitter.inject.utils.Handler
import com.twitter.server.Lifecycle.Warmup
import com.twitter.server.handler.ReplyHandler
import com.twitter.server.internal.{FinagleBuildRevision, PromoteToOldGenUtils}
import com.twitter.util.Await

/** AbstractTwitterServer for usage from Java */
abstract class AbstractTwitterServer extends TwitterServer

trait TwitterServer
  extends App
  with com.twitter.server.TwitterServer
  with Ports
  with Warmup
  with Logging {

  addFrameworkModules(
    statsModule,
    Slf4jBridgeModule)

  private val adminAnnounceFlag = flag[String]("admin.announce", "Address for announcing admin server")

  /* Protected */

  // TODO: Default to true
  override protected def failfastOnFlagsNotParsed = false

  /**
   * If true, the Twitter-Server admin server will be disabled.
   * Note: Disabling the admin server allows Finatra to be deployed into environments where only a single port is allowed
   */
  protected def disableAdminHttpServer: Boolean = false

  protected def statsModule: Module = StatsReceiverModule // TODO: Use Guice v4 OptionalBinder

  protected def slf4jBridgeModule: Module = Slf4jBridgeModule // TODO: Use Guice v4 OptionalBinder

  /** Resolve all Finagle clients before warmup method called */
  protected def resolveFinagleClientsOnStartup = true

  protected def waitForServer() {
    Await.ready(adminHttpServer)
  }

  /**
   * Utility to run a [[com.twitter.inject.utils.Handler]]. This is generally used for running
   * a warmup handler in #warmup.
   * @tparam T - type parameter with upper-bound of [[com.twitter.inject.utils.Handler]]
   * @see [[com.twitter.inject.utils.Handler]]
   * TODO: rename to handle[T <: Handler]()
   */
  protected def run[T <: Handler : Manifest]() {
    injector.instance[T].handle()
  }

  /* Overrides */

  override final def main() {
    super.main() // Call inject.App.main() to create injector

    info("Startup complete, server ready.")
    waitForServer()
  }

  /** Method to be called after injector creation */
  override protected def postStartup() {
    super.postStartup()

    if (resolveFinagleClientsOnStartup) {
      info("Resolving Finagle clients before warmup")
      Await.ready {
        ClientRegistry.expAllRegisteredClientsResolved() onSuccess { clients =>
          info("Done resolving clients: " + clients.mkString("[", ", ", "]") + ".")
        }
      }
    }

    FinagleBuildRevision.register(injector)
  }

  /**
   * After warmup completes, we want to run PromoteToOldGen without also signaling
   * that we're healthy since we haven't successfully started our servers yet
   */
  override protected def beforePostWarmup() {
    super.beforePostWarmup()
    PromoteToOldGenUtils.beforeServing()
  }

  override protected def postWarmup() {
    super.postWarmup()

    if (disableAdminHttpServer) {
      info("Disabling the Admin HTTP Server since disableAdminHttpServer=true")
      adminHttpServer.close()
    } else {
      for (addr <- adminAnnounceFlag.get) adminHttpServer.announce(addr)
    }
  }

  /**
   * After postWarmup, all external servers have been started, and we can now
   * enable our health endpoint
   */
  override protected def afterPostWarmup() {
    super.afterPostWarmup()
    info("Enabling health endpoint on port " + PortUtils.getPort(adminHttpServer))
    HttpMuxer.addHandler("/health", new ReplyHandler("OK\n"))
  }
}
