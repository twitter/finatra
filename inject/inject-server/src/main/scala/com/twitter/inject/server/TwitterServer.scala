package com.twitter.inject.server

import com.google.inject.Module
import com.twitter.finagle.client.ClientRegistry
import com.twitter.inject.Logging
import com.twitter.inject.annotations.Lifecycle
import com.twitter.inject.app.App
import com.twitter.inject.modules.StatsReceiverModule
import com.twitter.inject.utils.Handler
import com.twitter.server.Lifecycle.Warmup
import com.twitter.server.internal.FinagleBuildRevision
import com.twitter.util.{Awaitable, Await}
import java.util.concurrent.ConcurrentLinkedQueue
import scala.collection.JavaConverters._

/** AbstractTwitterServer for usage from Java */
abstract class AbstractTwitterServer extends TwitterServer

/**
 * A [[com.twitter.server.TwitterServer]] that supports injection and [[com.twitter.inject.TwitterModule]] modules.
 *
 * To use, override the appropriate @Lifecycle and callback method(s). Make sure when overriding @Lifecycle methods
 * to call the super implementation, otherwise critical lifecycle set-up  may not occur causing your server to either
 * function improperly or outright fail.
 *
 * Typically, you will only need to interact with the following methods:
 *
 *  postWarmup -- create and bind any external interface(s). See [[com.twitter.inject.app.App#postWarmup]]
 *  start -- callback executed after the injector is created and all @Lifecycle methods have completed.
 */
trait TwitterServer
  extends App
  with com.twitter.server.TwitterServer
  with Ports
  with Warmup
  with Logging {

  addFrameworkModules(
    statsReceiverModule)

  private val adminAnnounceFlag = flag[String]("admin.announce", "Address for announcing admin server")

  /* Mutable State */

  private[inject] val awaitables: ConcurrentLinkedQueue[Awaitable[_]] = new ConcurrentLinkedQueue()

  premain {
    awaitables.add(adminHttpServer)
  }

  /* Protected */

  override protected def failfastOnFlagsNotParsed: Boolean = true

  /**
   * Name used for registration in the [[com.twitter.util.registry.Library]]
   *
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
   *
   * @return a [[com.twitter.inject.TwitterModule]] which provides a [[com.twitter.finagle.stats.StatsReceiver]] implementation.
   */
  protected def statsReceiverModule: Module = StatsReceiverModule

  /** Resolve all Finagle clients before warmup method called */
  protected def resolveFinagleClientsOnStartup: Boolean = true

  protected def await[T <: Awaitable[_]](awaitable: T): Unit = {
    assert(awaitable != null, "Cannot call #await() on null Awaitable.")
    this.awaitables.add(awaitable)
  }

  protected def await(awaitables: Awaitable[_]*): Unit = {
    awaitables foreach await
  }

  /**
   * Utility to run a [[com.twitter.inject.utils.Handler]]. This is generally used for running
   * a warmup handler in #warmup.
   *
   * @tparam T - type parameter with upper-bound of [[com.twitter.inject.utils.Handler]]
   * @see [[com.twitter.inject.utils.Handler]]
   */
  protected def handle[T <: Handler : Manifest](): Unit = {
    injector.instance[T].handle()
  }

  /**
   * Callback method executed after the injector is created and all
   * lifecycle methods have fully completed. It is NOT expected that
   * you block in this method as you will prevent completion
   * of the server lifecycle.
   *
   * The server is signaled as STARTED prior to the execution of this
   * callback as all lifecycle methods have successfully completed and the
   * admin and any external interfaces have started.
   *
   * This method can be used to start long-lived processes that run in
   * separate threads from the main() thread. It is expected that you manage
   * these threads manually, e.g., by using a [[com.twitter.util.FuturePool]].
   *
   * Any exceptions thrown in this method will result in the server exiting.
   */
  protected def start(): Unit = {
  }

  /* Overrides */

  override final def main(): Unit = {
    super.main() // Call inject.App.main() to create injector

    info("Startup complete, server ready.")
    Await.all(awaitables.asScala.toSeq: _*)
  }

  /**
   * @see [[com.twitter.inject.server.TwitterServer#start]]
   */
  override final protected def run(): Unit = {
    start()
  }

  @Lifecycle
  override protected def postInjectorStartup(): Unit = {
    super.postInjectorStartup()

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
   * After warmup but before accepting traffic promote to old gen
   * (which triggers gc).
   *
   * @see [[com.twitter.server.Lifecycle.Warmup#prebindWarmup]]
   * @see [[com.twitter.inject.app.App#beforePostWarmup]]
   */
  @Lifecycle
  override protected def beforePostWarmup(): Unit = {
    super.beforePostWarmup()

    // trigger gc before accepting traffic
    prebindWarmup()
  }

  /**
   * If you override this method to create and bind any external interface or to
   * instantiate any awaitable it is expected that you add the Awaitable (or
   * [[com.twitter.finagle.ListeningServer]]) to the list of Awaitables using the
   * [[await[T <: Awaitable[_]](awaitable: T): Unit]] function.
   *
   * It is NOT expected that you block in this method as you will prevent completion
   * of the server lifecycle.
   */
  @Lifecycle
  override protected def postWarmup(): Unit = {
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
   * enable our health endpoint.
   *
   * @see [[com.twitter.server.Lifecycle.Warmup#warmupComplete]]
   * @see [[com.twitter.inject.app.App#afterPostwarmup]]
   */
  @Lifecycle
  override protected def afterPostWarmup(): Unit = {
    super.afterPostWarmup()

    if (!disableAdminHttpServer) {
      info("Enabling health endpoint on port " + PortUtils.getPort(adminHttpServer))
    }
    warmupComplete()
  }
}
