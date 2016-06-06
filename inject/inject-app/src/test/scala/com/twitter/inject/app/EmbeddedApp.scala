package com.twitter.inject.app

import com.google.inject.Stage
import com.twitter.inject.PoolUtils
import com.twitter.util._
import org.scalatest.Matchers

/**
 * EmbeddedApp allow's an App to be started locally for integration and feature testing.
 *
 * @param app The app to be started for testing
 * @param flags Command line flags (e.g. "foo"->"bar" is translated into -foo=bar)
 * @param resolverMap Resolver map entries (helper for creating the resolverMap clientFlag)
 * @param args Extra command line arguments
 * @param waitForWarmup Once the app is started, wait for App warmup to be completed.
 * @param skipAppMain Skip the running of appMain when the app starts. You will need to manually call app.appMain() later in your test.
 * @param stage [[com.google.inject.Stage]] used to create the server's injector. Since EmbeddedApp is used for testing, we default to Stage.DEVELOPMENT.
 *              This makes it possible to only mock objects that are used in a given test, at the expense of not checking that the entire
 *              object graph is valid. As such, you should always have at lease one Stage.PRODUCTION test for your service (which eagerly
 *              creates all classes at startup).
 * @param verbose Enable verbose logging during test runs
 * @param disableTestLogging Disable all logging emitted from the test infrastructure
 * @param maxStartupTimeSeconds Max seconds to wait for app startup
 */
class EmbeddedApp(
  app: com.twitter.app.App,
  flags: Map[String, String] = Map(),
  resolverMap: Map[String, String] = Map(),
  args: Seq[String] = Seq(),
  waitForWarmup: Boolean = true,
  skipAppMain: Boolean = false,
  stage: Stage = Stage.DEVELOPMENT,
  verbose: Boolean = false,
  disableTestLogging: Boolean = false,
  maxStartupTimeSeconds: Int = 60)
  extends Matchers {

  /* Additional Constructors */
  def this(app: com.twitter.app.App) = {
    this(app, stage = Stage.PRODUCTION)
  }

  /* Main Constructor */

  require(!isSingletonObject(app),
    "app must be a new instance rather than a singleton (e.g. \"new " +
      "FooServer\" instead of \"FooServerMain\" where FooServerMain is " +
      "defined as \"object FooServerMain extends FooServer\"")

  // overwrite com.google.inject.Stage if the underlying
  // embedded app is a com.twitter.inject.app.App.
  if (isInjectableApp) {
    injectableApp.stage = stage
  }

  /* Fields */

  val name = app.name
  private val mainRunnerFuturePool = PoolUtils.newFixedPool("Embedded " + name)

  //Mutable state
  private var starting = false
  private var started = false
  protected[inject] var closed = false
  private var _mainResult: Future[Unit] = _

  // This needs to be volatile because it is set
  // in mainRunnerFuturePool onFailure which is a
  // different thread than waitForAppStarted, where it's read.
  @volatile private var startupFailedThrowable: Option[Throwable] = None

  /* Public */

  lazy val isInjectableApp = app.isInstanceOf[App]
  lazy val injectableApp = app.asInstanceOf[App]
  lazy val injector = {
    start()
    injectableApp.injector
  }

  def mainResult: Future[Unit] = {
    start()
    if (_mainResult == null) {
      throw new Exception("Server needs to be started by calling EmbeddedApp#start()")
    }
    else {
      _mainResult
    }
  }

  def isStarted = started

  //NOTE: Start is called in various places to "lazily start the server" as needed
  def start() {
    if (!starting && !started) {
      starting = true //mutation

      if (isInjectableApp && skipAppMain) {
        injectableApp.runAppMain = false
      }

      runTwitterUtilAppMain()

      if (waitForWarmup) {
        waitForAppStarted()
      }

      started = true //mutation
      starting = false //mutation
    }
  }

  def appMain() {
    infoBanner("Run AppMain for class " + name)
    injectableApp.appMain()
  }

  def close() {
    if (!closed) {
      infoBanner(s"Closing ${this.getClass.getSimpleName}: " + name)
      try {
        Await.result(app.close())
        mainRunnerFuturePool.executor.shutdown()
      } catch {
        case e: Throwable =>
          info(s"Error while closing ${this.getClass.getSimpleName}: $e")
      }
      closed = true
    }
  }

  // Note: We avoid using slf4j info logging
  // so that we can differentiate server logs
  // vs test logs without requiring a test logging
  // configuration to be loaded
  def info(str: String): Unit = {
    if (!disableTestLogging) {
      println(str)
    }
  }

  def infoBanner(str: String) {
    info("\n")
    info("=" * 75)
    info(str)
    info("=" * 75)
  }

  /* Protected */

  protected def combineArgs(): Array[String] = {
    val clientFlagsStr = flagsAsArgs(updateFlags(flags))
    (args ++ clientFlagsStr).toArray
  }

  protected def updateFlags(map: Map[String, String]): Map[String, String] = {
    map
  }

  protected def logAppStartup() = {
    infoBanner("App warmup completed: " + name)
  }

  protected def nonInjectableAppStarted(): Boolean = {
    true
  }

  /* Private */

  private def runTwitterUtilAppMain() {
    // we call distinct here b/c port flag args can potentially be added multiple times
    val allArgs = combineArgs().distinct
    info("Starting " + name + " with args: " + allArgs.mkString(" "))

    _mainResult = mainRunnerFuturePool {
      try {
        app.nonExitingMain(allArgs)
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
      //If we rethrow, the exception will be suppressed by the Future Pool's monitor. Instead we save off the exception and rethrow outside the pool
      startupFailedThrowable = Some(e)
    }
  }

  private def flagsAsArgs(flagsMap: Map[String, String]) = {
    for ((key, value) <- flagsMap) yield {
      "-" + key + "=" + value
    }
  }

  private def isSingletonObject(app: com.twitter.app.App) = {
    import scala.reflect.runtime.currentMirror
    currentMirror.reflect(app).symbol.isModuleClass
  }

  private def waitForAppStarted() {
    for (i <- 1 to maxStartupTimeSeconds) {
      info("Waiting for warmup phases to complete...")

      if (startupFailedThrowable.isDefined) {
        println(s"\nEmbedded app $name failed to startup")
        throw startupFailedThrowable.get
      }

      if ((isInjectableApp && injectableApp.started) || (!isInjectableApp && nonInjectableAppStarted)) {
        started = true
        logAppStartup()
        return
      }

      Thread.sleep(1000)
    }
    throw new StartupTimeoutException(s"App: $name failed to startup within $maxStartupTimeSeconds seconds.")
  }
}
