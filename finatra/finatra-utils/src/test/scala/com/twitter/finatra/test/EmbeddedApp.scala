package com.twitter.finatra.test

import com.google.inject.util.Types
import com.google.inject.{Key, Stage, TypeLiteral}
import com.twitter.app.App
import com.twitter.finagle.stats.{InMemoryStatsReceiver, StatsReceiver}
import com.twitter.finatra.conversions.time._
import com.twitter.finatra.guice.GuiceApp
import com.twitter.finatra.test.Banner._
import com.twitter.finatra.utils.RetryUtils.retry
import com.twitter.finatra.utils.{Logging, RetryPolicyUtils}
import com.twitter.scrooge.ThriftService
import com.twitter.util._
import org.scalatest.Matchers

/**
 * EmbeddedApp is used for testing App's locally.
 *
 * @param app The app to be started for testing
 * @param clientFlags Command line flags (e.g. "foo"->"bar" is translated into -foo=bar)
 * @param resolverMap Resolver map entries (helper for creating the resolverMap clientFlag)
 * @param extraArgs Extra command line arguments
 * @param waitForWarmup Once the app is started, wait for App warmup to be completed.
 * @param skipAppMain Skip the running of appMain when the app starts. You will need to manually call app.appMain() later in your test.
 */
class EmbeddedApp(
  app: App,
  clientFlags: Map[String, String] = Map(),
  resolverMap: Map[String, String] = Map(),
  extraArgs: Seq[String] = Seq(),
  waitForWarmup: Boolean = true,
  skipAppMain: Boolean = false,
  stage: Stage = Stage.DEVELOPMENT)
  extends Matchers
  with Logging {

  /* Constructor */

  /*
   * Overwrite Guice stage if app is a GuiceApp.
   *
   * This allows optionally performing tests in Stage.PRODUCTION which eagerly creates
   * all Guice classes at startup.
   *
   * By default Stage.DEVELOPMENT is used which lazily creates objects. This makes it possible to
   * only mock objects that are used in a given test, at the expense of not checking that the
   * entire object graph is valid. As such, you should always have at lease one Stage.PRODUCTION
   * test for your service.
   */
  if (isGuiceApp) {
    guiceApp.guiceStage = stage
  }

  /* Fields */

  val appName = app.getClass.getName
  private val futurePool = FuturePool.unboundedPool

  //Mutable state
  private var started = false
  private var startupFailed = false
  protected[finatra] var closed = false
  private var _mainResult: Future[Unit] = _

  /* Override */

  override def finalize() {
    close() //close embedded GuiceApp
  }

  /* Public */

  lazy val isGuiceApp = app.isInstanceOf[GuiceApp]
  lazy val guiceApp = app.asInstanceOf[GuiceApp]
  lazy val injector = {
    start()
    guiceApp.injector
  }
  lazy val statsReceiver = if (isGuiceApp) injector.instance[StatsReceiver] else new InMemoryStatsReceiver
  lazy val inMemoryStatsReceiver = statsReceiver.asInstanceOf[InMemoryStatsReceiver]

  def mainResult = {
    start()
    if (_mainResult == null) {
      throw new Exception("Server needs to be started by calling EmbeddedApp#start()")
    }
    else {
      _mainResult
    }
  }

  //NOTE: Start is called in various places to "lazily start the server" as needed
  def start() {
    if (!started) {
      if (isGuiceApp && skipAppMain) {
        guiceApp.runAppMain = false
      }

      runTwitterUtilAppMain()

      if (waitForWarmup) {
        waitForWarmupComplete()
      }

      started = true //mutation
    }
  }

  /*
   * Note: The preferred solution is to use @Bind found in IntegrationTest. Only use this technique
   * when needing to test multiple EmbeddedTwitterServer's (since for now, IntegrationTest only supports a single server at a time...)
   *
   * Workaround for not being able to get a Manifest from a scrooge3 generated FooService[Future] which uses higher-kinded types :-/
   */
  def getThriftMock[T <: ThriftService : ClassManifest]: T = {
    val thriftType =
      Types.newParameterizedType(
        classManifest[T].erasure,
        classOf[Future[_]])

    val thriftKey = Key.get(TypeLiteral.get(thriftType))
    injector.instance(thriftKey).asInstanceOf[T]
  }

  private def runTwitterUtilAppMain() {
    val allArgs = combineArgs()
    println("Starting " + appName + " with args: " + allArgs.mkString(" "))

    _mainResult = futurePool {
      try {
        app.main(allArgs)
      } catch {
        case e: OutOfMemoryError if e.getMessage == "PermGen space" =>
          println("OutOfMemoryError(PermGen) in server startup. " +
            "This is most likely due to the incorrect setting of a client " +
            "flag (not defined or invalid). Increase your permgen to see the exact error message (e.g. -XX:MaxPermSize=256m)")
          e.printStackTrace()
          System.exit(-1)
        case e if !NonFatal.isNonFatal(e) =>
          println("Fatal exception in server startup.")
          throw new Exception(e) // Need to rethrow as a NonFatal for FuturePool to "see" the exception :/
      }
    } onFailure { e =>
      println("Error in embedded twitter-server thread ")
      e.printStackTrace()
      println()
      startupFailed = true //mutation
    }
  }

  def appMain() {
    banner("Run AppMain for " + appName)
    guiceApp.appMain()
  }

  def close() {
    if (!closed) {
      banner("Closing EmbeddedTwitterServer for " + appName)
      app.close()
      closed = true
    }
  }

  def clearStats() = {
    StatTestUtils.clear(inMemoryStatsReceiver)
  }

  def printStats() {
    banner(app.getClass.getSimpleName + " Server Stats")
    StatTestUtils.printStats(inMemoryStatsReceiver)
  }

  /* Protected */

  protected def combineArgs(): Array[String] = {
    (extraArgs ++ flagsStr(clientFlags) ++
      resolverMapStr(resolverMap)).toArray
  }

  /* Private */

  private def flagsStr(flagsMap: Map[String, String]) = {
    for ((key, value) <- flagsMap) yield {
      "-" + key + "=" + value
    }
  }

  private def resolverMapStr(resolverMap: Map[String, String]): Seq[String] = {
    if (resolverMap.isEmpty)
      Seq()
    else
      Seq(
        "-com.twitter.server.resolverMap=" + {
          resolverMap map { case (k, v) =>
            k + "=" + v
          } mkString ","
        })
  }

  private lazy val waitForStartupRetryPolicy = RetryPolicyUtils.constantRetry[Boolean](
    start = 1.second,
    numRetries = 300,
    shouldRetry = {case Return(started) => !started})

  def waitForWarmupComplete() {
    val started = retry(waitForStartupRetryPolicy, suppress = true) {
      if (startupFailed) {
        fail("Server startup failed")
      }

      println("Waiting for warmup phases to complete...")
      guiceApp.postWarmupComplete
    }

    if (!started.get()) {
      throw new Exception("App: %s failed to startup.".format(appName))
    }
    logAppStartup()
  }

  protected def logAppStartup() = {
    banner("App warmup completed (" + appName + ")")
  }

  private def isModuleLoaded(name: String): Boolean = {
    isGuiceApp &&
      (guiceApp.requiredModules.mkString contains name)
  }
}
