package com.twitter.inject.server

import com.google.inject.Module
import com.twitter.app.Flag
import com.twitter.conversions.DurationOps._
import com.twitter.finagle.client.ClientRegistry
import com.twitter.inject.Logging
import com.twitter.inject.annotations.Lifecycle
import com.twitter.inject.app.App
import com.twitter.inject.modules.StatsReceiverModule
import com.twitter.inject.modules.internal.LibraryModule
import com.twitter.inject.utils.Handler
import com.twitter.server.Lifecycle.Warmup
import com.twitter.server.internal.FinagleBuildRevision
import com.twitter.util.lint.{Category, GlobalRules, Issue, Rule}
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

  addFrameworkModules(
    statsReceiverModule,
    new LibraryModule(libraryName)
  )

  private val adminAnnounceFlag =
    flag[String]("admin.announce", "Address for announcing admin server")

  /* Mutable State */

  private[inject] val awaitables: ConcurrentLinkedQueue[Awaitable[_]] = new ConcurrentLinkedQueue()

  premain {
    awaitables.add(adminHttpServer)
  }

  /* Protected */

  /**
   * Name used for registration in the [[com.twitter.util.registry.Library]]
   *
   * @return library name to register in the Library registry.
   */
  override protected def libraryName: String = "finatra"

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

  /**
   * Callback to register an [[Awaitable]] instance for the server to await (block) on.
   *
   * All registered [[Awaitable]] instances are entangled by the server such that if any
   * registered [[Awaitable]] exits it will trigger all registered [[Awaitable]] instances to exit.
   * @param awaitable an [[Awaitable]] instance to register.
   * @see [[https://twitter.github.io/finatra/user-guide/twitter-server/index.html#awaiting-awaitables Awaiting Awaitables]]
   */
  protected def await[T <: Awaitable[_]](awaitable: T): Unit = {
    assert(awaitable != null, "Cannot call #await() on null Awaitable.")
    debug(s"Adding ${awaitable.getClass.getName} to list of Awaitables")
    this.awaitables.add(awaitable)
  }

  /**
   * Callback to register multiple [[Awaitable]] instances for the server to await (block) on.
   * @param awaitables vararg list of [[Awaitable]] instances to register.
   * @see [[TwitterServer.await(awaitable: Awaitable)]]
   */
  protected def await(awaitables: Awaitable[_]*): Unit = {
    awaitables.foreach(await)
  }

  /**
   * Utility to run a [[com.twitter.inject.utils.Handler]]. This is generally used for running
   * a warmup handler in [[TwitterServer.warmup]].
   *
   * @tparam T - type parameter with upper-bound of [[com.twitter.inject.utils.Handler]]
   * @see [[com.twitter.inject.utils.Handler]]
   */
  protected def handle[T <: Handler: Manifest](): Unit = {
    injector.instance[T].handle()
  }

  /**
   * Utility to run a [[com.twitter.inject.utils.Handler]]. This is generally used for running
   * a warmup handler in [[TwitterServer.warmup]].
   *
   * @see [[com.twitter.inject.utils.Handler]]
   */
  protected def handle(clazz: Class[_ <: Handler]): Unit = {
    injector.instance(clazz).handle()
  }

  /* Overrides */

  override final def main(): Unit = {
    super[App].main() // Call inject.App.main() to create Injector

    info("Startup complete, server awaiting.")
    Awaiter.any(awaitables.asScala, period = 1.second)
    info("Awaited awaitables have exited, server done.")
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
    super[App].postInjectorStartup()

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
   * Callback method run before [[TwitterServer.postWarmup]], used for performing warm up of this server.
   * Override, but do not call `super.warmup()` as you will trigger a lint rule violation.
   *
   * Any exceptions thrown in this method will result in the app exiting.
   *
   * @see [[https://twitter.github.io/finatra/user-guide/http/warmup.html HTTP Server Warmup]]
   * @see [[https://twitter.github.io/finatra/user-guide/thrift/warmup.html Thrift Server Warmup]]
   */
  override protected def warmup(): Unit = {
    // If this method is not overridden with an implementation,
    // emit a lint rule violation to warn users that they SHOULD
    // provide a warmup implementation.
    GlobalRules.get.add(
      Rule(
        Category.Performance,
        "No server warm up detected",
        "It is highly recommended that services perform some type of warm up of their " +
          "external interfaces to mitigate any impact on success rate upon accepting traffic " +
          "after server startup."
      ) {
        Seq(Issue("No warm up implementation detected."))
      }
    )
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
    super[App].beforePostWarmup()

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
    super[App].postWarmup()

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
    super[App].afterPostWarmup()

    if (!disableAdminHttpServer) {
      info("admin http server started on port " + PortUtils.getPort(adminHttpServer))
    }
    warmupComplete()
  }

  /**
   * @see [[com.twitter.inject.server.TwitterServer#start]]
   */
  override final protected def run(): Unit = {
    start()
  }

  /**
   * Callback method which is executed after the injector is created and all
   * lifecycle methods have fully completed but before awaiting on any Awaitables.
   * It is NOT expected that you block in this method as you will prevent completion
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
}

@deprecated("For backwards compatibility of defined flags", "2017-10-06")
private[server] trait DeprecatedLogging extends com.twitter.logging.Logging { self: App =>
  @deprecated("For backwards compatibility only.", "2017-10-06")
  override lazy val log: com.twitter.logging.Logger = com.twitter.logging.Logger(name)

  // lint if any com.twitter.logging.Logging flags are set
  premain {
    val flgs: Seq[Flag[_]] =
      Seq(
        inferClassNamesFlag,
        outputFlag,
        levelFlag,
        asyncFlag,
        asyncMaxSizeFlag,
        rollPolicyFlag,
        appendFlag,
        rotateCountFlag
      )

    val userDefinedFlgs: Seq[Flag[_]] = flgs.collect {
      case flg: Flag[_] if flg.isDefined => flg
    }

    if (userDefinedFlgs.nonEmpty && !userDefinedFlgsAllowed) {
      GlobalRules.get.add(
        Rule(
          Category.Configuration,
          "Unsupported util-logging (JUL) flag set",
          """By default, Finatra uses the slf4j-api for logging and as such setting util-logging
            | flags is not expected to have any effect. Setting these flags may cause your server to
            | fail startup in the future. Logging configuration should always match your chosen logging
            | implementation.
            | See: https://twitter.github.io/finatra/user-guide/logging/index.html.""".stripMargin
        ) {
          userDefinedFlgs.map(flg => Issue(s"-${flg.name}"))
        }
      )
    }
  }

  /** If slf4j-jdk14 is being used, it is acceptable to have user defined values for these flags */
  private[this] def userDefinedFlgsAllowed: Boolean = {
    try {
      Class.forName("org.slf4j.impl.JDK14LoggerFactory", false, this.getClass.getClassLoader)
      true
    } catch {
      case _: ClassNotFoundException => false
    }
  }

  /**
   * [[com.twitter.logging.Logging.configureLoggerFactories()]] removes all added JUL handlers
   * and adds only handlers defined by [[com.twitter.logging.Logging.loggerFactories]].
   *
   * `Logging.configureLoggerFactories` would thus remove the installed SLF4J BridgeHandler
   * from [[com.twitter.server.TwitterServer]]. Therefore, we override with a no-op to prevent the
   * SLF4J BridgeHandler from being removed.
   *
   * @note Subclasses MUST override this method with an implementation that configures the
   *       `com.twitter.logging.Logger` if they want to use their configured logger factories via
   *       the util-logging style of configuration.
   *
   * @see [[https://www.slf4j.org/legacy.html#jul-to-slf4j jul-to-slf4j bridge]]
   */
  override protected def configureLoggerFactories(): Unit = {}
}
