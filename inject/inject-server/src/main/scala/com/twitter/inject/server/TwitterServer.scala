package com.twitter.inject.server

import com.google.inject.Module
import com.twitter.finagle.client.ClientRegistry
import com.twitter.inject.Logging
import com.twitter.inject.app.App
import com.twitter.inject.modules.StatsReceiverModule
import com.twitter.inject.utils.Handler
import com.twitter.server.Lifecycle.Warmup
import com.twitter.server.internal.FinagleBuildRevision
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
    statsModule)

  private val adminAnnounceFlag = flag[String]("admin.announce", "Address for announcing admin server")

  /* Protected */

  // TODO: Default to true
  override protected def failfastOnFlagsNotParsed: Boolean = false

  /**
   * Name used for registration in the [[com.twitter.util.registry.Library]]
   * @return library name to register in the Library registry.
   */
  override protected val libraryName: String = "finatra"

  /**
   * If true, the Twitter-Server admin server will be disabled.
   * Note: Disabling the admin server allows services to be deployed into environments where only a single port is allowed
   */
  protected def disableAdminHttpServer: Boolean = false

  /**
   * Default [[com.twitter.inject.TwitterModule]] for providing a [[com.twitter.finagle.stats.StatsReceiver]].
   * @return a [[com.twitter.inject.TwitterModule]] which provides a [[com.twitter.finagle.stats.StatsReceiver]] implementation.
   */
  protected def statsModule: Module = StatsReceiverModule // TODO: Use Guice v4 OptionalBinder

  /** Resolve all Finagle clients before warmup method called */
  protected def resolveFinagleClientsOnStartup: Boolean = true

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

  override protected def beforePostWarmup() {
    super.beforePostWarmup()

    // trigger gc before accepting traffic
    prebindWarmup()
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
    warmupComplete()
  }
}
