package com.twitter.inject.server

import com.google.inject.Stage
import com.twitter.finagle.stats.{InMemoryStatsReceiver, StatsReceiver}
import com.twitter.inject.{Injector, PoolUtils}
import com.twitter.inject.app.{BindDSL, StartupTimeoutException}
import com.twitter.inject.conversions.map._
import com.twitter.inject.modules.InMemoryStatsReceiverModule
import com.twitter.inject.server.PortUtils.getPort
import com.twitter.util.lint.{GlobalRules, Rule}
import com.twitter.util.{Await, Closable, Duration, ExecutorServiceFuturePool, Future}
import java.util.concurrent.ConcurrentLinkedQueue
import org.scalatest.Matchers
import scala.collection.JavaConverters._
import scala.collection.SortedMap
import scala.util.control.NonFatal

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
  private def isSingletonObject(server: com.twitter.server.TwitterServer) = {
    import scala.reflect.runtime.currentMirror
    currentMirror.reflect(server).symbol.isModuleClass
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
  closeGracePeriod: Option[Duration] = None
) extends AdminHttpClient(twitterServer, verbose)
  with BindDSL
  with Matchers {

  import EmbeddedTwitterServer._

  /* Additional Constructors */

  def this(twitterServer: Ports, flags: java.util.Map[String, String], stage: Stage) =
    this(twitterServer, flags = flags.asScala.toMap, stage = stage)

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
    injectableServer.addFrameworkOverrideModules(InMemoryStatsReceiverModule)
  }

  /* Fields */

  val name: String = twitterServer.name
  val EmbeddedName: String = embeddedName(name)
  private[this] val FuturePoolName: String = s"finatra/embedded/$EmbeddedName"
  private[this] val futurePool: ExecutorServiceFuturePool = PoolUtils.newFixedPool(FuturePoolName)
  private[this] val closables: ConcurrentLinkedQueue[Closable] = new ConcurrentLinkedQueue()

  /* Mutable state */

  private[this] var starting: Boolean = false
  private[this] var started: Boolean = false
  protected[inject] var closed: Boolean = false
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

  lazy val statsReceiver: StatsReceiver =
    if (isInjectable) injector.instance[StatsReceiver]
    else new InMemoryStatsReceiver
  lazy val inMemoryStatsReceiver: InMemoryStatsReceiver =
    statsReceiver.asInstanceOf[InMemoryStatsReceiver]
  lazy val adminHostAndPort: String = PortUtils.loopbackAddressForPort(httpAdminPort())

  /* Public */

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
  def isStarted: Boolean = started

  /**
   * Start the underlying TwitterServer.
   * @note Start is called in various places to "lazily start the server" as needed.
   */
  def start(): Unit = {
    if (!starting && !started) {
      starting = true //mutation

      runNonExitingMain()

      if (waitForWarmup) {
        waitForServerStarted()
      }

      started = true //mutation
      starting = false //mutation
    }
  }

  /** Assert the underlying TwitterServer has started */
  def assertStarted(started: Boolean = true): Unit = {
    assert(isInjectable)
    start()
    injectableServer.started should be(started)
  }

  /**
   * Assert that the underlying TwitterServer is "healthy". This will attempt to hit
   * the AdminHttpInterface `/health` endpoint and expects an "OK\n" response body.
   *
   * This will throw an Exception if the health assertion fails.
   * @see [[isHealthy]] to determine the underlying TwitterServer health without an Exception.
   *
   * @param healthy what value for health to assert, e.g., to assert that the server is
   *                "unhealthy", `assertHealthy(false)`. To assert that the server is
   *                "healthy", `assertHealthy(true)`, Default is true.
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
    if (!closed) {
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
          info(s"Error while closing ${this.getClass.getSimpleName}: ${e.getMessage}\n", disableLogging)
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
        closed = true
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
      throw new IllegalStateException(s"$name is not closed")
    } else if (shutdownFailure.isDefined) {
      throw shutdownFailure.get
    }
  }

  /* StatsReceiver Functions */

  def clearStats(): Unit = {
    inMemoryStatsReceiver.counters.clear()
    inMemoryStatsReceiver.stats.clear()
    inMemoryStatsReceiver.gauges.clear()
  }

  def statsMap: SortedMap[String, Seq[Float]] =
    inMemoryStatsReceiver.stats.iterator.toMap.mapKeys(keyStr).toSortedMap
  def countersMap: SortedMap[String, Long] =
    inMemoryStatsReceiver.counters.iterator.toMap.mapKeys(keyStr).toSortedMap
  def gaugeMap: SortedMap[String, () => Float] =
    inMemoryStatsReceiver.gauges.iterator.toMap.mapKeys(keyStr).toSortedMap

  def printStats(includeGauges: Boolean = true): Unit = {
    infoBanner(name + " Stats", disableLogging)
    for ((key, values) <- statsMap) {
      val avg = values.sum / values.size
      val valuesStr = values.mkString("[", ", ", "]")
      info(f"$key%-70s = $avg = $valuesStr", disableLogging)
    }

    info("\nCounters:", disableLogging)
    for ((key, value) <- countersMap) {
      info(f"$key%-70s = $value", disableLogging)
    }

    if (includeGauges) {
      info("\nGauges:", disableLogging)
      for ((key, value) <- inMemoryStatsReceiver.gauges.iterator.toMap
          .mapKeys(keyStr)
          .toSortedMap) {
        info(f"$key%-70s = ${value()}", disableLogging)
      }
    }
  }

  def getCounter(name: String): Long = {
    countersMap.getOrElse(name, 0)
  }

  def assertCounter(name: String, expected: Long): Unit = {
    getCounter(name) should equal(expected)
  }

  def assertCounter(name: String)(callback: Long => Boolean): Unit = {
    callback(getCounter(name)) should be(true)
  }

  def getStat(name: String): Seq[Float] = {
    statsMap.getOrElse(name, Seq())
  }

  def assertStat(name: String, expected: Seq[Float]): Unit = {
    getStat(name) should equal(expected)
  }

  def getGauge(name: String): Float = {
    gaugeMap.get(name) map { _.apply() } getOrElse 0f
  }

  def assertGauge(name: String, expected: Float): Unit = {
    val value = getGauge(name)
    value should equal(expected)
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

  protected def nonInjectableServerStarted(): Boolean = isHealthy

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

  /* StatsReceiver key utility */
  private def keyStr(keys: Seq[String]): String = {
    keys.mkString("/")
  }

  private def flagsAsArgs(flags: Map[String, String]): Iterable[String] = {
    flags.map { case (k, v) => "-" + k + "=" + v }
  }

  private def runNonExitingMain(): Unit = {
    // we call distinct here b/c port flag args can potentially be added multiple times
    val allArgs = combineArgs().distinct
    info("\nStarting " + name + " with args: " + allArgs.mkString(" "), disableLogging)

    _mainResult = futurePool {
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
          throw new Exception(e) // Need to rethrow as a NonFatal for FuturePool to "see" the exception :/
      }
    }.onFailure { e =>
      //If we rethrow, the exception will be suppressed by the Future Pool's monitor. Instead we save off the exception and rethrow outside the pool
      startupFailedThrowable = Some(e)
    }
  }

  private def throwStartupFailedException(): Unit = {
    println(s"\nEmbedded server $name failed to startup: ${startupFailedThrowable.get.getMessage}")
    throw startupFailedThrowable.get
  }

  private def waitForServerStarted(): Unit = {
    for (_ <- 1 to maxStartupTimeSeconds) {
      info("Waiting for warmup phases to complete...", disableLogging)

      if (startupFailedThrowable.isDefined) {
        throwStartupFailedException()
      }

      if ((isInjectable && injectableServer.started)
        || (!isInjectable && nonInjectableServerStarted)) {
        /* TODO: RUN AND WARN ALWAYS
           For now only run if failOnValidation = true until
           we allow for a better way to isolate the server startup
           in feature tests */
        if (failOnLintViolation) {
          checkStartupLintIssues()
        }

        started = true
        logStartup()
        return
      }

      Thread.sleep(1000)
    }
    throw new StartupTimeoutException(
      s"Embedded server: $name failed to startup within $maxStartupTimeSeconds seconds."
    )
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
