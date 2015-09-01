package com.twitter.inject.server

import com.google.inject.Module
import com.twitter.finagle.client.ClientRegistry
import com.twitter.finagle.{http, httpx}
import com.twitter.inject.Logging
import com.twitter.inject.app.App
import com.twitter.inject.modules.StatsReceiverModule
import com.twitter.server.Lifecycle.Warmup
import com.twitter.server.internal.{FinagleBuildRevision, PromoteToOldGenUtils}
import com.twitter.util.Await

trait TwitterServer
  extends com.twitter.server.TwitterServer
  with Ports
  with App
  with Warmup
  with Logging {

  addFrameworkModule(statsModule)

  /* Protected */

  protected def statsModule: Module = StatsReceiverModule // TODO: Use Guice v4 OptionalBinder

  /** Resolve all Finagle clients before warmup method called */
  protected def resolveFinagleClientsOnStartup = true

  /* Overrides */

  override final def main() {
    super.main() // Call GuiceApp.main() to create injector

    info("Startup complete, server ready.")
    Await.ready(adminHttpServer)
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

  /**
   * After postWarmup, all external servers have been started, and we can now
   * enable our health endpoint
   */
  override protected def afterPostWarmup() {
    super.afterPostWarmup()
    info("Enabling health endpoint on port " + httpAdminPort)

    /* For our open-source release, we need to stay backwards compatible with Finagle 6.28.0 and Twitter-Server 1.13.0 */
    // TODO: Remove usage of http.HttpMuxer after new twitter-server released
    if (httpx.HttpMuxer.patterns.contains("/health"))
      httpx.HttpMuxer.addHandler("/health", new HttpxReplyHandler("OK\n"))
    else
      http.HttpMuxer.addHandler("/health", new HttpReplyHandler("OK\n"))
  }
}
