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
import scala.reflect.{ClassTag, classTag}

/**
 * EmbeddedApp allow's an App to be started locally for integration and feature testing.
 *
 * @param app The app to be started for testing
 * @param clientFlags Command line flags (e.g. "foo"->"bar" is translated into -foo=bar)
 * @param resolverMap Resolver map entries (helper for creating the resolverMap clientFlag)
 * @param extraArgs Extra command line arguments
 * @param waitForWarmup Once the app is started, wait for App warmup to be completed.
 * @param skipAppMain Skip the running of appMain when the app starts. You will need to manually call app.appMain() later in your test.
 * @param stage Guice Stage used to create the server's injector. Since EmbeddedApp is used for testing, we default to Stage.DEVELOPMENT.
 *              This makes it possible to only mock objects that are used in a given test, at the expense of not checking that the entire
 *              object graph is valid. As such, you should always have at lease one Stage.PRODUCTION test for your service (which eagerly
 *              creates all Guice classes at startup).
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

  require(!isSingletonObject(app),
    "app must be a new instance rather than a singleton (e.g. \"new " +
      "FooServer\" instead of \"FooServerMain\" where FooServerMain is " +
      "defined as \"object FooServerMain extends FooServer\"")

  /*
   * Overwrite Guice stage if app is a GuiceApp.
   */
  if (isGuiceApp) {
    guiceApp.guiceStage = stage
  }

  /* Fields */

  val appName = app.getClass.getName
  private val futurePool = FuturePool.unboundedPool

  //Mutable state
  private var started = false
  private var startupFailedMessage: Option[String] = None
  protected[finatra] var closed = false
  private var _mainResult: Future[Unit] = _

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
   * Workaround for not being able to get a Manifest from a scrooge3 generated FooService[Future] which uses higher-kinded types :-/
   *
   * Note: The preferred solution is to use @Bind found in IntegrationTest. Only use this technique
   * when needing to test multiple EmbeddedTwitterServer's (since for now, IntegrationTest only supports a single server at a time...)
   */
  def getThriftMock[T <: ThriftService : ClassTag]: T = {
    val thriftType =
      Types.newParameterizedType(
        classTag[T].runtimeClass,
        classOf[Future[_]])

    val thriftKey = Key.get(TypeLiteral.get(thriftType))
    injector.instance(thriftKey).asInstanceOf[T]
  }

  def appMain() {
    banner("Run AppMain for class " + appName)
    guiceApp.appMain()
  }

  def close() {
    if (!closed) {
      banner("Closing EmbeddedApp for class " + appName)
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

  protected def logAppStartup() = {
    banner("App warmup completed (" + appName + ")")
  }

  /* Private */

  private def runTwitterUtilAppMain() {
    val allArgs = combineArgs()
    println("Starting " + appName + " with args: " + allArgs.mkString(" "))

    _mainResult = futurePool {
      try {
        //TODO: Switch to app.nonExitingMain once next version of util-core is open-source released
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
      startupFailedMessage = Some(e.getMessage)
    }
  }

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

  // hack
  private def isSingletonObject(app: App) = {
    app.getClass.getSimpleName.endsWith("$")
  }

  private lazy val waitForStartupRetryPolicy = RetryPolicyUtils.constantRetry[Boolean](
    start = 1.second,
    numRetries = 300,
    shouldRetry = {case Return(started) => !started})

  private def waitForWarmupComplete() {
    val started = retry(waitForStartupRetryPolicy, suppress = true) {
      startupFailedMessage.map(msg => fail(s"Server startup failed on exception=$msg"))

      println("Waiting for warmup phases to complete...")
      if (isGuiceApp) {
        guiceApp.postWarmupComplete
      }
      else {
        nonGuiceWarmupComplete()
      }
    }

    if (!started.get()) {
      throw new Exception("App: %s failed to startup.".format(appName))
    }
    logAppStartup()
  }

  protected def nonGuiceWarmupComplete(): Boolean = {
    true
  }
}
