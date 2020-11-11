package com.twitter.inject.server

import com.google.inject.Stage
import com.twitter.app.GlobalFlag
import com.twitter.app.lifecycle.Event.{Close, PreMain}
import com.twitter.app.lifecycle.{Event, Observer}
import com.twitter.conversions.DurationOps._
import com.twitter.finagle.stats.{InMemoryStatsReceiver, StatsReceiver}
import com.twitter.finagle.util.DefaultTimer
import com.twitter.inject.app.{BindDSL, StartupTimeoutException}
import com.twitter.inject.conversions.map._
import com.twitter.inject.modules.InMemoryStatsReceiverModule
import com.twitter.inject.server.PortUtils.getPort
import com.twitter.inject.{Injector, InMemoryStatsReceiverUtility, PoolUtils, TwitterModule}
import com.twitter.util.lint.{GlobalRules, Rule}
import com.twitter.util.{
  Await,
  Closable,
  Duration,
  ExecutorServiceFuturePool,
  Future,
  Promise,
  TimeoutException
}
import java.lang.annotation.Annotation
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.ConcurrentLinkedQueue
import scala.collection.JavaConverters._
import scala.collection.SortedMap
import scala.util.control.NonFatal
import org.scalatest.matchers.should.Matchers

object EmbeddedTwitterServer {
  private def resolveFlags(useSocksProxy: Boolean, flags: Map[String, String]) = {
    if (useSocksProxy) {
      flags ++ Map(
        "com.twitter.server.resolverZkHosts" -> PortUtils.loopbackAddressForPort(2181),
        "com.twitter.finagle.socks.socksProxyHost" -> PortUtils.loopbackAddress,
        "com.twitter.finagle.socks.socksProxyPort" -> "50001"
      )
    } else {
      flags
    }
  }

  /**
   * Returns true if the given instance is a Scala singleton object, false otherwise.
   * @see [[https://docs.scala-lang.org/tour/singleton-objects.html]]
   */
  private def isSingletonObject(server: com.twitter.server.TwitterServer): Boolean = {
    // while Scala's reflection utilities offer this in a succinct and convenient
    // method, the startup costs are quite high for quick testing iteration cycles:
    //
    //    scala.reflect.runtime.currentMirror.reflect(server).symbol.isModuleClass
    //
    // the approach used here, while fragile to Scala internal changes, was deemed
    // worth the tradeoff. this assumes that companion objects have class names that
    // end with $ and have a field on them called MODULE$. For example `object MyServer`
    // has the class name `MyServer$` and a `MODULE$` field. this works as of Scala 2.12.
    val clazz = server.getClass
    val className = clazz.getName
    if (!className.endsWith("$"))
      return false

    try {
      clazz.getField("MODULE$") // this throws if the field doesn't exist
      true
    } catch {
      case _: NoSuchFieldException => false
    }
  }

  /**
   * Lowercases, trims whitespace, replaces all "-" with empty string, removes spaces. If given
   * a fully qualified classname, will pull out everything between the last "." and the first "$"
   * (if present, to the end of the string otherwise).
   *
   * E.g., "com.twitter.finatra.Foo$anon$$" would return "foo".
   */
  private def embeddedName(name: String): String = {
    val firstPass = name.trim.replaceAll("-", "").replaceAll(" ", "").toLowerCase
    if (firstPass.contains(".")) {
      val endIndex =
        if (firstPass.contains("$") && firstPass.lastIndexOf(".") < firstPass.indexOf("$"))
          firstPass.indexOf("$")
        else firstPass.length
      firstPass.substring(firstPass.lastIndexOf(".") + 1, endIndex)
    } else {
      firstPass
    }
  }

  /**
   * A type alias that represents a function that generates a `T` as input (`=> T`)
   * and outputs a `T` result. This signature allows for reducing
   * multiple functions of `(=> T) => T` into a single `(=> T) => T`.
   *
   * @note The `(=> T)` allows for passing a lazy executed function as an
   *       input function. If the signature were `T => T`, then the
   *       input would be eagerly executed if it were actually a `=> T`.
   *
   * @note This is `private[server]` scoped for testing purposes
   * @tparam T The input and output type of the function
   */
  private[server] type ReducibleFn[T] = (=> T) => T

  /**
   * Takes an ordered sequence of functions that require scoping over an
   * underlying input. The first function (i.e. the head) will be scoped
   * closest to the input to the resulting function.
   *
   * @param fns The ordered functions to be reduced to a single function
   * @tparam T The input/output type of the functions
   *
   * @return A single function comprised of the `fns` functions
   *
   * @note This is `private[server]` scoped for testing purposes
   */
  private[server] def mkGlobalFlagsFn[T](fns: Iterable[ReducibleFn[T]]): ReducibleFn[T] =
    fns.reduce[ReducibleFn[T]] {
      case (fn, collector) =>
        input => collector(fn(input))
    }

  /**
   * Generate a [[Promise]] that will be fulfilled when the server has bound and exposed a specific
   * port that is ready for traffic. The resulting [[Promise]] can be [[Await awaited]] upon to
   * gate execution of a client against an [[EmbeddedTwitterServer]], as the server may be considered
   * started and ready for testing before ports are bound for a specific client.
   *
   * @param twitterServer The [[com.twitter.server.TwitterServer]] to observe
   * @param portBound The logic to exercise that will determine if the specific port is ready
   *                  and bound within the underlying [[twitterServer]].
   * @return A [[Promise]] that will be fulfilled when the port is bound to the [[twitterServer]]
   *
   * @note The [[portBound]] logic will never exercised if the [[twitterServer]] is an
   *       [[TwitterServer inject TwitterServer]], as the lifecycle information notifies us when the
   *       ports are bound and ready.
   * @note This method should be called before attempting to start the underlying [[twitterServer]].
   */
  private[twitter] def isPortReady(
    twitterServer: com.twitter.server.TwitterServer,
    portBound: => Boolean
  ): Promise[Unit] = {
    val promise = Promise[Unit]()

    val warmupObserver: Observer = new Observer {

      private[this] val (readyEvent, readyAction) = twitterServer match {
        case t: com.twitter.inject.server.TwitterServer =>
          (t.startupCompletionEvent, () => promise.setDone())
        case _ =>
          // this is a special case where cannot observe the lifecycle to know when ports are bound
          // and need to poll until the port is assigned
          (PreMain, () => waitForPorts())
      }

      // sleep loop for case where we cannot observe the lifecycle for a maximum of 5 seconds
      private[this] def waitForPorts(remaining: Int = 100): Future[Unit] =
        if (portBound) {
          promise.setDone()
          promise
        } else if (remaining == 0) {
          promise.setException(new StartupTimeoutException("Port was not exposed"))
          promise
        } else {
          Future.sleep(50.millis)(DefaultTimer).before(waitForPorts(remaining - 1))
        }

      override def onSuccess(event: Event): Unit = event match {
        case e if e == readyEvent =>
          readyAction()
        case Close =>
          promise.setDone()
        case _ =>
          () // do nothing
      }

      override def onFailure(
        stage: Event,
        throwable: Throwable
      ): Unit = promise.setDone()
    }

    twitterServer.withObserver(warmupObserver)
    promise
  }
}

/**
 * EmbeddedTwitterServer allows a [[com.twitter.server.TwitterServer]] serving http or thrift endpoints to be started
 * locally (on ephemeral ports) and tested through it's http/thrift interfaces.
 *
 * Note: All initialization fields are lazy to aid running multiple tests inside an IDE at the same time
 * since IDEs typically "pre-construct" ALL the tests before running each one.
 *
 * @param twitterServer The [[com.twitter.server.TwitterServer]] to be started for testing.
 * @param flags Command line flags (e.g. "foo"->"bar" is translated into -foo=bar). See: [[com.twitter.app.Flag]].
 * @param args Extra command line arguments.
 * @param waitForWarmup Once the server is started, wait for server warmup to be completed
 * @param stage [[com.google.inject.Stage]] used to create the server's injector. Since EmbeddedTwitterServer is used for testing,
 *              we default to Stage.DEVELOPMENT. This makes it possible to only mock objects that are used in a given test,
 *              at the expense of not checking that the entire object graph is valid. As such, you should always have at
 *              least one Stage.PRODUCTION test for your service (which eagerly creates all classes at startup)
 * @param useSocksProxy Use a tunneled socks proxy for external service discovery/calls (useful for manually run external
 *                      integration tests that connect to external services).
 * @param defaultRequestHeaders Headers to always send to the embedded server.
 * @param streamResponse Toggle to not unwrap response content body to allow caller to stream response.
 * @param verbose Enable verbose logging during test runs.
 * @param disableTestLogging Disable all logging emitted from the test infrastructure.
 * @param maxStartupTimeSeconds Maximum seconds to wait for embedded server to start. If exceeded a
 *                              [[com.twitter.inject.app.StartupTimeoutException]] is thrown.
 * @param failOnLintViolation If server startup should fail due (and thus the test) to a detected lint rule issue after startup.
 * @param closeGracePeriod An Optional grace period to use instead of the underlying server's
 *                         `defaultGracePeriod` when closing the underlying server.
 * @param globalFlags An ordered map of [[GlobalFlag]] and the desired value to be set during the
 *                    scope of the underlying [[twitterServer]]'s lifecycle. The flags will be
 *                    applied in insertion order, with the first entry being applied closest to
 *                    the startup of the [[twitterServer]]. In order to ensure insertion ordering,
 *                    you should use a [[scala.collection.immutable.ListMap]].
 * @param statsReceiverOverride An optional [[StatsReceiver]] implementation that should be bound to the
 *                              underlying server when testing with an injectable server. By default
 *                              an injectable server under test will have an [[InMemoryStatsReceiver]] implementation
 *                              bound for the purpose of testing. In some cases, users may want to test using
 *                              a custom [[StatsReceiver]] implementation instead and can provide an instance
 *                              to use here. For non-injectable servers this can be a shared reference
 *                              used in the server under test.
 */
class EmbeddedTwitterServer(
  twitterServer: com.twitter.server.TwitterServer,
  flags: => Map[String, String] = Map(),
  args: => Seq[String] = Seq(),
  waitForWarmup: Boolean = true,
  stage: Stage = Stage.DEVELOPMENT,
  useSocksProxy: Boolean = false,
  val defaultRequestHeaders: Map[String, String] = Map(),
  val streamResponse: Boolean = false,
  verbose: Boolean = false,
  disableTestLogging: Boolean = false,
  maxStartupTimeSeconds: Int = 60,
  failOnLintViolation: Boolean = false,
  closeGracePeriod: Option[Duration] = None,
  globalFlags: => Map[GlobalFlag[_], String] = Map(),
  statsReceiverOverride: Option[StatsReceiver] = None)
    extends AdminHttpClient(twitterServer, verbose)
    with BindDSL
    with Matchers {

  import EmbeddedTwitterServer._

  /* Additional Constructors */

  def this(twitterServer: Ports, flags: java.util.Map[String, String], stage: Stage) =
    this(twitterServer, flags = flags.asScala.toMap, stage = stage)

  def this(
    twitterServer: Ports,
    flags: java.util.Map[String, String],
    globalFlags: java.util.Map[GlobalFlag[_], String],
    stage: Stage
  ) =
    this(
      twitterServer,
      flags = flags.asScala.toMap,
      stage = stage,
      globalFlags = globalFlags.toOrderedMap)

  /* Main Constructor */

  require(
    !isSingletonObject(twitterServer),
    "server must be a new instance rather than a singleton (e.g. \"new " +
      "FooServer\" instead of \"FooServerMain\" where FooServerMain is " +
      "defined as \"object FooServerMain extends FooServer\""
  )

  if (isInjectable) {
    // overwrite com.google.inject.Stage if the underlying
    // embedded server is a com.twitter.inject.server.TwitterServer.
    injectableServer.stage = stage
    // Add framework override modules
    statsReceiverOverride match {
      case Some(receiver) =>
        injectableServer.addFrameworkOverrideModules(new TwitterModule {
          override def configure(): Unit = {
            bind[StatsReceiver].toInstance(receiver)
          }
        })
      case _ =>
        injectableServer.addFrameworkOverrideModules(InMemoryStatsReceiverModule)
    }
  }

  /* Fields */

  val name: String = twitterServer.name
  val EmbeddedName: String = embeddedName(name)

  private[this] val FuturePoolName: String = s"finatra/embedded/$EmbeddedName"
  private[this] val futurePool: ExecutorServiceFuturePool = PoolUtils.newFixedPool(FuturePoolName)

  private[this] val closables: ConcurrentLinkedQueue[Closable] = new ConcurrentLinkedQueue()

  // start() ends up calling itself, thus we want to bypass/skip if we are already starting
  private[this] val starting: AtomicBoolean = new AtomicBoolean(false)
  private[this] val started: AtomicBoolean = new AtomicBoolean(false)
  private[this] val _closed: AtomicBoolean = new AtomicBoolean(false)
  protected[inject] def closed: Boolean = _closed.get

  // this latch ticks down when the server is ready to take requests or has failed to start
  private[this] val ready: Promise[Unit] = Promise[Unit]()

  // observer of the underlying `twitterServer`'s lifecycle events, so that we can accurately
  // tick down the `countdownLatch` based on the `twitterServer`'s marked startup completion event.
  private[this] val lifecycleObserver = new Observer {

    override def onSuccess(event: Event): Unit =
      if (!isStarted && event == twitterServer.startupCompletionEvent) {
        ready.setDone()
      }

    override def onFailure(
      stage: Event,
      throwable: Throwable
    ): Unit = {
      if (stage == Close) {
        shutdownFailure = Some(throwable)
      } else {
        startupFailedThrowable = Some(throwable)
      }
      ready.setDone()
    }
  }
  twitterServer.withObserver(lifecycleObserver)

  /* Mutable state */

  private[this] var _mainResult: Future[Unit] = _
  // This needs to be volatile because it is set in futurePool onFailure
  // which is a different thread than waitForServerStarted, where it's read.
  @volatile private[this] var startupFailedThrowable: Option[Throwable] = None
  private[this] var shutdownFailure: Option[Throwable] = None

  /* Lazy Fields */

  lazy val isInjectable: Boolean = twitterServer.isInstanceOf[TwitterServer]
  lazy val injectableServer: TwitterServer = twitterServer.asInstanceOf[TwitterServer]

  lazy val injector: Injector = {
    start()
    injectableServer.injector
  }

  /** Returns the [[StatsReceiver]] for the underlying server when applicable */
  lazy val statsReceiver: StatsReceiver =
    if (isInjectable) injector.instance[StatsReceiver]
    else
      statsReceiverOverride.getOrElse(throw new IllegalStateException(
        "Accessing the underlying StatsReceiver is only supported with an injectable server or when an override is provided."))

  lazy val usesInMemoryStatsReceiver: Boolean =
    statsReceiver.isInstanceOf[InMemoryStatsReceiver]

  /**
   * Returns the bound [[InMemoryStatsReceiver]] when applicable. If access to this member is
   * attempted when a non [[InMemoryStatsReceiver]] is provided as a [[statsReceiverOverride]]
   * this will throw an [[IllegalStateException]] as it is not expected that users call this
   * when providing a custom [[StatsReceiver]] implementation via the [[statsReceiverOverride]].
   */
  lazy val inMemoryStatsReceiver: InMemoryStatsReceiver = statsReceiver match {
    case receiver: InMemoryStatsReceiver => receiver
    case _ =>
      throw new IllegalStateException(
        "The configured StatsReceiver implementation is not of type InMemoryStatsReceiver.")
  }

  /** @throws IllegalStateException when a non [[InMemoryStatsReceiver]] is provided as a [[statsReceiverOverride]]. */
  lazy val inMemoryStats: InMemoryStatsReceiverUtility =
    new InMemoryStatsReceiverUtility(inMemoryStatsReceiver)

  lazy val adminHostAndPort: String = PortUtils.loopbackAddressForPort(httpAdminPort())

  /* Public */

  // java-forwarder methods - DO NOT override except for creating a java-forwarder method
  // that only calls `super()`
  override def bindClass[T](clazz: Class[T], instance: T): this.type =
    super.bindClass[T](clazz, instance)

  // java-forwarder methods - DO NOT override except for creating a java-forwarder method
  // that only calls `super()`
  override def bindClass[T](clazz: Class[T], annotation: Annotation, instance: T): this.type =
    super.bindClass[T](clazz, annotation, instance)

  // java-forwarder methods - DO NOT override except for creating a java-forwarder method
  // that only calls `super()`
  override def bindClass[T, Ann <: Annotation](
    clazz: Class[T],
    annotationClazz: Class[Ann],
    instance: T
  ): this.type =
    super.bindClass[T, Ann](clazz, annotationClazz, instance)

  // java-forwarder methods - DO NOT override except for creating a java-forwarder method
  // that only calls `super()`
  override def bindClass[T, U <: T](clazz: Class[T], instanceClazz: Class[U]): this.type =
    super.bindClass[T, U](clazz, instanceClazz)

  // java-forwarder methods - DO NOT override except for creating a java-forwarder method
  // that only calls `super()`
  override def bindClass[T, U <: T](
    clazz: Class[T],
    annotation: Annotation,
    instanceClazz: Class[U]
  ): this.type =
    super.bindClass[T, U](clazz, annotation, instanceClazz)

  // java-forwarder methods - DO NOT override except for creating a java-forwarder method
  // that only calls `super()`
  override def bindClass[T, Ann <: Annotation, U <: T](
    clazz: Class[T],
    annotationClazz: Class[Ann],
    instanceClazz: Class[U]
  ): this.type =
    super.bindClass[T, Ann, U](clazz, annotationClazz, instanceClazz)

  /**
   * Returns the result of running the `nonExitingMain` of the underlying TwitterServer in
   * the embedded Future Pool.
   */
  def mainResult: Future[Unit] = {
    start()
    if (_mainResult == null) {
      throw new Exception("Server needs to be started by calling EmbeddedTwitterServer#start()")
    } else {
      _mainResult
    }
  }

  def httpAdminPort(): Int = {
    start()
    getPort(twitterServer.adminBoundAddress)
  }

  /**
   * If the underlying embedded TwitterServer has started.
   * @return True if the server has started, False otherwise.
   */
  def isStarted: Boolean = started.get

  /**
   * Start the underlying TwitterServer.
   * @note Start is called in various places to "lazily start the server" as needed.
   */
  def start(): Unit = {
    if (starting.compareAndSet(false, true)) {
      runNonExitingMain()

      if (waitForWarmup) {
        waitForServerStarted()
      }
      logStartup()
      started.set(true)
    }

    // if there is/was an exception on startup, we want it to be thrown *every* time
    // this method is called.
    throwIfStartupFailed()
  }

  /** If the [[startupFailedThrowable]] is defined, [[throwStartupFailedException]] */
  private def throwIfStartupFailed(): Unit =
    if (startupFailedThrowable.isDefined) throwStartupFailedException()

  /** Assert the underlying TwitterServer has started */
  def assertStarted(started: Boolean = true): Unit = {
    assert(isInjectable)
    start()
    isStarted should be(true)
  }

  /**
   * Assert that the underlying TwitterServer is "healthy". This will attempt to hit
   * the AdminHttpInterface `/health` endpoint and expects an "OK\n" response body.
   *
   * This will throw a [[org.scalatest.exceptions.TestFailedException]] if the health assertion fails.
   * @see [[isHealthy]] to determine the underlying TwitterServer health without an Exception.
   *
   * @param healthy what value for health to assert, e.g., to assert that the server is
   *                "healthy", [[assertHealthy(healthy=true)]], Default is true.
   * @note In almost no case is there a need to [[assertHealthy(healthy=false)]] as this
   *       would typically equate to the underlying server failing to startup -- which would result
   *       in a startup exception. If you want to build a wait-loop for the server to report itself
   *       as healthy, it is better to query the admin `/health` endpoint directly using [[isHealthy]].
   */
  def assertHealthy(healthy: Boolean = true): Unit = {
    healthResponse(healthy).get()
  }

  /**
   * Determines if the underlying TwitterServer is "healthy". This will attempt to hit
   * the AdminHttpInterface `/health` endpoint and expects an "OK\n" response body.
   *
   * @return True is the server is "healthy", False otherwise.
   */
  def isHealthy: Boolean = {
    httpAdminPort() != 0 && healthResponse().isReturn
  }

  /**
   * Close the EmbeddedTwitterServer. This closes the underlying TwitterServer, any other
   * [[Closable]] instances registered with `closeOnExit`, and shuts down the FuturePool used to run
   * the underlying TwitterServer. This method returns when all resources have been fully relinquished.
   *
   * @see [[EmbeddedTwitterServer.close(after: Duration)]]
   */
  def close(): Unit = {
    close(closeGracePeriod.getOrElse(twitterServer.defaultCloseGracePeriod))
  }

  /**
   * Close the EmbeddedTwitterServer with the given timeout. This timeout is advisory, giving the
   * callee some leeway, for example to drain clients or finish up other tasks. This closes the
   * underlying TwitterServer, any other [[Closable]] instances registered with `closeOnExit`, and
   * shuts down the FuturePool used to run the underlying TwitterServer. This method returns when
   * all resources have been fully relinquished.
   */
  def close(after: Duration): Unit = {
    if (_closed.compareAndSet(false, true)) {
      infoBanner(s"Closing ${this.getClass.getSimpleName}: " + name, disableLogging)
      try {
        val underlyingClosable = Closable.make { deadline =>
          info(s"Closing underlying TwitterServer: $name", disableLogging)
          twitterServer.close(deadline)
        }
        closables.add(underlyingClosable)
        Await.result(Future.collect(closables.asScala.toIndexedSeq.map(_.close(after))))
      } catch {
        case NonFatal(e) =>
          info(
            s"Error while closing ${this.getClass.getSimpleName}: ${e.getMessage}\n",
            disableLogging
          )
          e.printStackTrace()
          shutdownFailure = Some(e)
      } finally {
        try {
          info(s"Shutting down Future Pool: $FuturePoolName", disableLogging)
          futurePool.executor.shutdown()
        } catch {
          case t: Throwable =>
            info(s"Unable to shutdown $FuturePoolName future pool executor. $t", disableLogging)
            t.printStackTrace()
        }
      }
    }
  }

  /**
   * Asserts that no NonFatal exception was caught while shutting down the underlying
   * `TwitterServer`. If an exception occurred on close of the underlying server
   * it will be thrown here.
   */
  def assertCleanShutdown(): Unit = {
    if (!closed) {
      throw new IllegalStateException(s"$name is not closed.")
    } else if (shutdownFailure.isDefined) {
      throw shutdownFailure.get
    }
  }

  /* InMemoryStatsReceiver Functions */

  /**
   * Prints stats from the bound [[InMemoryStatsReceiver]] when applicable. If access to this method
   * is attempted when a non [[InMemoryStatsReceiver]] is provided as a [[statsReceiverOverride]]
   * this will throw an [[IllegalStateException]] as it is not expected that users call this
   * when providing a custom [[StatsReceiver]] implementation via the [[statsReceiverOverride]].
   * Instead, users should prefer to print stats from their custom [[StatsReceiver]] implementation
   * by other means.
   *
   * @throws IllegalStateException when a non [[InMemoryStatsReceiver]] is provided as a
   *        [[statsReceiverOverride]].
   */
  def printStats(): Unit = {
    infoBanner(name + " Stats", disableLogging)
    if (!disableLogging) inMemoryStats.print()
  }

  /**
   * Clears all metrics of the bound [[InMemoryStatsReceiver]] when applicable. If access to this
   * method is attempted when a non [[InMemoryStatsReceiver]] is provided as a [[statsReceiverOverride]]
   * this will throw an [[IllegalStateException]] as it is not expected that users call this
   * when providing a custom [[StatsReceiver]] implementation via the [[statsReceiverOverride]].
   * Instead, users should prefer to clear stats from their custom [[StatsReceiver]] implementation
   * by other means.
   *
   * @throws IllegalStateException when a non [[InMemoryStatsReceiver]] is provided as a
   *         [[statsReceiverOverride]].
   */
  def clearStats(): Unit = {
    inMemoryStats.clear()
  }

  /** @throws IllegalStateException when a non [[InMemoryStatsReceiver]] is provided as a [[statsReceiverOverride]]. */
  @deprecated("Use the inMemoryStatsReceiverUtility#statsMap directly", "2019-05-17")
  def statsMap: SortedMap[String, Seq[Float]] =
    inMemoryStats.stats.toSortedMap

  /** @throws IllegalStateException when a non [[InMemoryStatsReceiver]] is provided as a [[statsReceiverOverride]]. */
  @deprecated("Use the inMemoryStatsReceiverUtility#countersMap directly", "2019-05-17")
  def countersMap: SortedMap[String, Long] =
    inMemoryStats.counters.toSortedMap

  /** @throws IllegalStateException when a non [[InMemoryStatsReceiver]] is provided as a [[statsReceiverOverride]]. */
  @deprecated("Use the inMemoryStatsReceiverUtility#gaugeMap directly", "2019-05-17")
  def gaugeMap: SortedMap[String, () => Float] =
    inMemoryStats.gauges.toSortedMap.map { case (k, v) => k -> (() => v) }

  /**
   * Prints stats from the bound [[InMemoryStatsReceiver]] when applicable. If access to this method
   * is attempted when a non [[InMemoryStatsReceiver]] is provided as a [[statsReceiverOverride]]
   * this will throw an [[IllegalStateException]] as it is not expected that users call this
   * when providing a custom [[StatsReceiver]] implementation via the [[statsReceiverOverride]].
   *
   * Instead users should prefer to print stats from their custom StatsReceiver implementation
   * by other means.
   *
   * @throws IllegalStateException when a non [[InMemoryStatsReceiver]] is provided as a
   *        [[statsReceiverOverride]].
   */
  @deprecated(
    "Use printStats(). The `includeGauges` parameter is ignored and gauge values will now always be printed from the underlying InMemoryStatsReceiver.",
    "2019-05-17"
  )
  def printStats(includeGauges: Boolean): Unit = this.printStats()

  /** @throws IllegalStateException when a non [[InMemoryStatsReceiver]] is provided as a [[statsReceiverOverride]]. */
  @deprecated("Use the inMemoryStatsReceiverUtility#counters#apply", "2019-05-17")
  def getCounter(name: String): Long = {
    inMemoryStats.counters.get(name).getOrElse(0)
  }

  /** @throws IllegalStateException when a non [[InMemoryStatsReceiver]] is provided as a [[statsReceiverOverride]]. */
  @deprecated("Use the inMemoryStatsReceiverUtility#counters#assert", "2019-05-17")
  def assertCounter(name: String, expected: Long): Unit = {
    getCounter(name) should equal(expected)
  }

  /** @throws IllegalStateException when a non [[InMemoryStatsReceiver]] is provided as a [[statsReceiverOverride]]. */
  @deprecated("Use the inMemoryStatsReceiverUtility#counters#assert", "2019-05-17")
  def assertCounter(name: String)(callback: Long => Boolean): Unit = {
    callback(getCounter(name)) should be(true)
  }

  /** @throws IllegalStateException when a non [[InMemoryStatsReceiver]] is provided as a [[statsReceiverOverride]]. */
  @deprecated("Use the inMemoryStatsReceiverUtility#stats#apply", "2019-05-17")
  def getStat(name: String): Seq[Float] = {
    inMemoryStats.stats.get(name).getOrElse(Seq.empty[Float])
  }

  /** @throws IllegalStateException when a non [[InMemoryStatsReceiver]] is provided as a [[statsReceiverOverride]]. */
  @deprecated("Use the inMemoryStatsReceiverUtility#stats#assert", "2019-05-17")
  def assertStat(name: String, expected: Seq[Float]): Unit = {
    getStat(name) should equal(expected)
  }

  /** @throws IllegalStateException when a non [[InMemoryStatsReceiver]] is provided as a [[statsReceiverOverride]]. */
  @deprecated("Use the inMemoryStatsReceiverUtility#gauges#apply", "2019-05-17")
  def getGauge(name: String): Float = {
    inMemoryStats.gauges.get(name).getOrElse(0)
  }

  /** @throws IllegalStateException when a non [[InMemoryStatsReceiver]] is provided as a [[statsReceiverOverride]]. */
  @deprecated("Use the inMemoryStatsReceiverUtility#gauges#assert", "2019-05-17")
  def assertGauge(name: String, expected: Float): Unit = {
    getGauge(name) should equal(expected)
  }

  /* Protected */

  /** Add the given [[Closable]] to the list of closables to be closed when the EmbeddedTwitterServer is closed */
  protected def closeOnExit(closable: Closable): Unit = {
    this.closables.add(closable)
  }

  protected def disableLogging: Boolean = {
    disableTestLogging || System.getProperties
      .keySet()
      .contains("com.twitter.inject.test.logging.disabled")
  }

  /** Log that the underlying embedded TwitterServer has started and the location of the AdminHttpInterface */
  protected[twitter] def logStartup(): Unit = {
    infoBanner("Server Started: " + name, disableLogging)
    info(s"AdminHttp      -> http://$adminHostAndPort/admin", disableLogging)
  }

  /** Combine the flags Map with the args String to create an argument list for the underlying embedded TwitterServer main */
  protected[twitter] def combineArgs(): Array[String] = {
    val flagsStr =
      flagsAsArgs(resolveFlags(useSocksProxy, flags))
    ("-admin.port=" + PortUtils.ephemeralLoopback) +: (args ++ flagsStr).toArray
  }

  override final protected def addInjectionServiceModule(module: com.google.inject.Module): Unit = {
    if (!isInjectable) {
      throw new IllegalStateException("Cannot call bind() with a non-injectable underlying server.")
    }
    injectableServer.addFrameworkOverrideModules(module)
  }

  /* Private */

  private def flagsAsArgs(flags: Map[String, String]): Iterable[String] = {
    flags.map { case (k, v) => "-" + k + "=" + v }
  }

  /**
   * A method that allows for applying [[com.twitter.util.Local]] context
   * (e.g. [[com.twitter.app.GlobalFlag]]) scoped to the execution of the
   * underlying [[twitterServer]]'s lifecycle.
   *
   * @example
   *          {{{
   *            override protected[twitter] def withLocals(fn: => Unit): Unit = super.withLocals {
   *              SomeLocal.let("xyz")(fn)
   *            }
   *          }}}
   *
   * @note Ordering of [[com.twitter.util.Local]] context scoping is important. The scope
   *       closest to the `fn` param is going to win, if there are conflicts.
   *
   * @param fn The main function that will take into account local scope modifications
   */
  protected[twitter] def withLocals(fn: => Unit): Unit =
    if (globalFlags.isEmpty) {
      fn
    } else {
      info(s"Applying GlobalFlag scoping to $name embedded server: $globalFlags")

      // take the globalFlags and map them to their local scoped functions
      val globalFlagLocalFns: Iterable[ReducibleFn[Unit]] = globalFlags.map {
        case (k, v) =>
          val f: ReducibleFn[Unit] = func =>
            k.letParse[Unit](v) {
              info(s"Applying GlobalFlag: ${k.name}=$v")
              func
            }
          f
      }

      // reduce all of the functions down to a single input function that can accept `fn`
      val globalsFn = mkGlobalFlagsFn(globalFlagLocalFns)

      // apply `fn` wrapped by GlobalFlag's local context modifications
      globalsFn(fn)
    }

  private[this] def runNonExitingMain(): Unit = {
    val allArgs = combineArgs()
    info("\nStarting " + name + " with args: " + allArgs.mkString(" "), disableLogging)

    _mainResult = futurePool {
      withLocals {
        try {
          twitterServer.nonExitingMain(allArgs)
        } catch {
          case e: OutOfMemoryError if e.getMessage == "PermGen space" =>
            println(
              "OutOfMemoryError(PermGen) in server startup. " +
                "This is most likely due to the incorrect setting of a client " +
                "flag (not defined or invalid). Increase your PermGen to see the exact error message (e.g. -XX:MaxPermSize=256m)"
            )
            e.printStackTrace()
            System.exit(-1)
          case e if !NonFatal(e) =>
            println("Fatal exception in server startup.")
            throw new Exception(
              e
            ) // Need to rethrow as a NonFatal for FuturePool to "see" the exception :/
        }
      }
    }.onFailure { e =>
      // If we rethrow, the exception will be suppressed by the Future Pool's monitor.
      // Instead we save off the exception and rethrow outside the pool
      startupFailedThrowable = Some(e)
    }
  }

  private def throwStartupFailedException(): Unit = {
    println(s"\nEmbedded server $name failed to startup: ${startupFailedThrowable.get.getMessage}")
    throw startupFailedThrowable.get
  }

  private def waitForServerStarted(): Unit = {
    info("Waiting for warmup phases to complete...", disableLogging)

    try {
      Await.ready(ready, maxStartupTimeSeconds.seconds)
    } catch {
      case e: TimeoutException =>
        throw new StartupTimeoutException(
          s"Embedded server: $name failed to startup within $maxStartupTimeSeconds seconds.",
          e)
    }

    throwIfStartupFailed()

    /* TODO: RUN AND WARN ALWAYS
        For now only run if failOnValidation = true until
        we allow for a better way to isolate the server startup
        in feature tests */
    if (failOnLintViolation) {
      checkStartupLintIssues()
    }

    started.set(true)
  }

  private def checkStartupLintIssues(): Unit = {
    val failures: Map[Rule, Seq[String]] = computeLintIssues
    val numIssues = failures.map(_._2.size).sum
    val issueString = if (numIssues == 1) "Issue" else "Issues"
    if (failures.nonEmpty) {
      info(s"Warning: $numIssues Linter $issueString Found!", disableLogging)
      failures.foreach {
        case (rule, issues) =>
          info(s"\t* Rule: ${rule.name} - ${rule.description}", disableLogging)
          issues.foreach(issue => info(s"\t - $issue", disableLogging))
      }
      info(
        "After addressing these issues, consider enabling failOnLintViolation mode to prevent future issues from reaching production.",
        disableLogging
      )
      if (failOnLintViolation) {
        val e = new Exception(
          s"failOnLintViolation is enabled and $numIssues Linter ${issueString.toLowerCase()} found."
        )
        startupFailedThrowable = Some(e)
        throwStartupFailedException()
      }
    }
  }

  private def computeLintIssues: Map[Rule, Seq[String]] = {
    val rules = GlobalRules.get.iterable.toSeq
    rules
      .map(rule => rule -> rule().map(_.details.replace("\n", " ").trim))
      .filterNot(_._2.isEmpty)
      .toMap
  }
}
