package com.twitter.inject.server

import com.google.inject.Module
import com.twitter.conversions.time._
import com.twitter.finagle.client.ClientRegistry
import com.twitter.inject.Logging
import com.twitter.inject.annotations.Lifecycle
import com.twitter.inject.app.App
import com.twitter.inject.modules.StatsReceiverModule
import com.twitter.inject.utils.Handler
import com.twitter.server.Lifecycle.Warmup
import com.twitter.server.internal.FinagleBuildRevision
import com.twitter.util.{Await, Awaitable}
import java.util.concurrent.ConcurrentLinkedQueue
import scala.collection.JavaConverters._

/** AbstractTwitterServer for usage from Java */
abstract class AbstractTwitterServer extends TwitterServer

/**
 * A [[com.twitter.server.TwitterServer]] that supports injection and [[com.twitter.inject.TwitterModule]]
 * modules.
 *
 * To use, override the appropriate @Lifecycle and callback method(s). Make sure when overriding
 * {{{@Lifecycle}}} methods to call the super implementation, otherwise critical lifecycle set-up  may not
 * occur causing your server to either function improperly or outright fail.
 *
 * If you are extending this trait, to implement your server, override the start() function, e.g.,
 *
 * {{{
 *   import com.twitter.inject.server.TwitterServer
 *
 *   object MyServerMain extends MyServer
 *
 *   class MyServer extends TwitterServer {
 *     override protected def start(): Unit = {
 *        // YOUR CODE HERE
 *
 *        await(someAwaitable)
 *     }
 *   }
 * }}}
 *
 * Note, you do not need to await on the `adminHttpServer` as this is done for you by the framework.
 *
 * Server Lifecycle:
 * +-------------------------------------------------------------------------+
 * | Life Cycle Method             | Ancillary Function(s)                   |
 * +-------------------------------------------------------------------------+
 * | loadModules()                 |                                         |
 * +-------------------------------------------------------------------------+
 * | modules.postInjectorStartup() | foreach.singletonStartup()              |
 * +-------------------------------------------------------------------------+
 * | postInjectorStartup()         | resolveFinagleClientsOnStartup(),       |
 * |                               | FinagleBuildRevision.register(),        |
 * |                               | setup()                                 |
 * +-------------------------------------------------------------------------+
 * | warmup()                      |                                         |
 * +-------------------------------------------------------------------------+
 * | beforePostWarmup()            | LifeCycle.Warmup.prebindWarmup()        |
 * +-------------------------------------------------------------------------+
 * | postWarmup() (binds ext ports)| disable or announce admin server        |
 * +-------------------------------------------------------------------------+
 * | afterpostwarmup()             | LifeCycle.Warmup.warmupComplete()       |
 * +-------------------------------------------------------------------------+
 * | setAppStarted()               |                                         |
 * +-------------------------------------------------------------------------+
 * | run()                         | start()                                 |
 * +-------------------------------------------------------------------------+
 * | Await on awaitables                                                     |
 * +-------------------------------------------------------------------------+
 * @see [[https://twitter.github.io/finatra/user-guide/twitter-server/index.html Creating an Injectable TwitterServer]]
 */
trait TwitterServer
  extends App
  with com.twitter.server.TwitterServer
  with DeprecatedLogging
  with Ports
  with Warmup
  with Logging {

  addFrameworkModules(statsReceiverModule)

  private val adminAnnounceFlag =
    flag[String]("admin.announce", "Address for announcing admin server")

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
    awaitables.foreach(await)
  }

  /**
   * Utility to run a [[com.twitter.inject.utils.Handler]]. This is generally used for running
   * a warmup handler in #warmup.
   *
   * @tparam T - type parameter with upper-bound of [[com.twitter.inject.utils.Handler]]
   * @see [[com.twitter.inject.utils.Handler]]
   */
  protected def handle[T <: Handler: Manifest](): Unit = {
    injector.instance[T].handle()
  }

  /**
   * Callback method which is executed specifically in the `postInjectorStartup` lifecycle
   * phase of this server.
   *
   * This is AFTER the injector is created but BEFORE server warmup has been performed.
   *
   * This method is thus suitable for starting and awaiting on PubSub publishers or subscribers.
   *
   * The server is NOT signaled to be started until AFTER this method has executed
   * thus it is imperative that this method is NOT BLOCKED as it will cause the server to not
   * complete startup.
   *
   * This method can be used to start long-lived processes that run in
   * separate threads from the main() thread. It is expected that you manage
   * these threads manually, e.g., by using a [[com.twitter.util.FuturePool]].
   *
   * If you override this method to instantiate any [[com.twitter.util.Awaitable]] it is expected
   * that you add the [[com.twitter.util.Awaitable]] to the list of `Awaitables` using the
   * [[await[T <: Awaitable[_]](awaitable: T): Unit]] function if you want the server to exit
   * when the [[com.twitter.util.Awaitable]] exits.
   *
   * Any exceptions thrown in this method will result in the server exiting.
   */
  protected def setup(): Unit = {}

  /**
   * Callback method which is executed after the injector is created and all
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
  protected def start(): Unit = {}

  /* Overrides */

  override final def main(): Unit = {
    super.main() // Call inject.App.main() to create Injector

    info("Startup complete, server ready.")
    Awaiter.any(awaitables.asScala, period = 1.second)
  }

  /**
   * @see [[com.twitter.inject.server.TwitterServer#start]]
   */
  override final protected def run(): Unit = {
    start()
  }

  /**
   * After creation of the Injector. Before any other lifecycle methods.
   *
   * @note You MUST call `super.postInjectorStartup()` in any overridden definition of this
   * method. Failure to do so may cause your server to not completely startup.
   *
   * @note It is NOT expected that you block in this method as you will prevent completion
   * of the server lifecycle.
   */
  @Lifecycle
  override protected def postInjectorStartup(): Unit = {
    super.postInjectorStartup()

    if (resolveFinagleClientsOnStartup) {
      info("Resolving Finagle clients before warmup")
      /* This PURPOSELY BLOCKS on a Future that will complete when Finagle client resolution is
       completed. We purposely await on client resolution here as we want to BLOCK server startup
       on the client resolution for clients to be fully resolved in time for the warmup phase. It
       is not expected that overrides of this method perform any other blocking that will prevent
       server startup from completing. */
      Await.ready {
        ClientRegistry.expAllRegisteredClientsResolved().onSuccess { clients =>
          info("Done resolving clients: " + clients.mkString("[", ", ", "]") + ".")
        }
      }
    }

    FinagleBuildRevision.register(injector)

    // run any setup logic
    setup()
  }

  /**
   * After warmup but before accepting traffic promote to old gen
   * (which triggers gc).
   *
   * @note You MUST call `super.beforePostWarmup()` in any overridden definition of this
   * method. Failure to do so may cause your server to not completely startup.
   *
   * @note It is NOT expected that you block in this method as you will prevent completion
   * of the server lifecycle.
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
   * @note You MUST call `super.postWarmup()` in any overridden definition of this
   * method. Failure to do so may cause your server to not completely startup.
   *
   * @note It is NOT expected that you block in this method as you will prevent completion
   * of the server lifecycle.
   */
  @Lifecycle
  override protected def postWarmup(): Unit = {
    super.postWarmup()

    if (disableAdminHttpServer) {
      info("Disabling the Admin HTTP Server since disableAdminHttpServer=true")
      awaitables.remove(adminHttpServer)
      adminHttpServer.close()
    } else {
      for (addr <- adminAnnounceFlag.get) adminHttpServer.announce(addr)
    }
  }

  /**
   * After postWarmup, all external servers have been started, and we can now
   * enable our health endpoint.
   *
   * @note You MUST call `super.afterPostWarmup()` in any overridden definition of this
   * method. Failure to do so may cause your server to not completely startup.
   *
   * @note It is NOT expected that you block in this method as you will prevent completion
   * of the server lifecycle.
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

@deprecated("For backwards compatibility of defined flags", "2017-10-06")
private[server] trait DeprecatedLogging extends com.twitter.logging.Logging { self: App =>
  @deprecated("For backwards compatibility only.", "2017-10-06")
  override lazy val log: com.twitter.logging.Logger = com.twitter.logging.Logger(name)

  override def configureLoggerFactories(): Unit = {}
}
