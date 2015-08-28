package com.twitter.inject.app

import com.google.inject.Stage
import com.twitter.inject.Logging
import com.twitter.inject.app.Banner.banner
import com.twitter.util._
import org.scalatest.Matchers

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
  app: com.twitter.app.App,
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

  val appName = app.name
  private val futurePool = FuturePool.unboundedPool

  //Mutable state
  private var started = false
  private var startupFailedThrowable: Option[Throwable] = None
  protected[inject] var closed = false
  private var _mainResult: Future[Unit] = _

  /* Public */

  lazy val isGuiceApp = app.isInstanceOf[App]
  lazy val guiceApp = app.asInstanceOf[App]
  lazy val injector = {
    start()
    guiceApp.injector
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

  //NOTE: Start is called in various places to "lazily start the server" as needed
  def start() {
    if (!started) {
      if (isGuiceApp && skipAppMain) {
        guiceApp.runAppMain = false
      }

      runTwitterUtilAppMain()

      if (waitForWarmup) {
        waitForAppStarted()
      }

      started = true //mutation
    }
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

  /* Protected */

  protected def combineArgs(): Array[String] = {
    (extraArgs ++ flagsStr(clientFlags)).toArray
  }

  protected def logAppStartup() = {
    banner("App warmup completed: " + appName)
  }

  /* Private */

  private def runTwitterUtilAppMain() {
    val allArgs = combineArgs()
    println("Starting " + appName + " with args: " + allArgs.mkString(" "))

    _mainResult = futurePool {
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

  private def flagsStr(flagsMap: Map[String, String]) = {
    for ((key, value) <- flagsMap) yield {
      "-" + key + "=" + value
    }
  }

  // hack
  private def isSingletonObject(app: com.twitter.app.App) = {
    app.getClass.getSimpleName.endsWith("$")
  }

  private def waitForAppStarted() {
    for (i <- 1 to 60) {
      println("Waiting for warmup phases to complete...")

      if (startupFailedThrowable.isDefined) {
        println(s"\nEmbedded app $appName failed to startup")
        throw startupFailedThrowable.get
      }

      if ((isGuiceApp && guiceApp.appStarted) || (!isGuiceApp && nonGuiceAppStarted())) {
        started = true
        logAppStartup()
        return
      }

      Thread.sleep(1000)
    }
    throw new scala.Exception(s"App: $appName failed to startup within 60 seconds.")
  }

  protected def nonGuiceAppStarted(): Boolean = {
    true
  }
}
