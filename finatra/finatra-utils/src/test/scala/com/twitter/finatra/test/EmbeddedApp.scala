package com.twitter.finatra.test

import com.google.inject.util.Types
import com.google.inject.{Key, TypeLiteral}
import com.twitter.app.App
import com.twitter.finagle.service.RetryPolicy
import com.twitter.finagle.stats.{InMemoryStatsReceiver, StatsReceiver}
import com.twitter.finatra.conversions.time._
import com.twitter.finatra.guice.GuiceApp
import com.twitter.finatra.test.Banner._
import com.twitter.finatra.utils.{Logging, RetryPolicyUtils}
import com.twitter.scrooge.ThriftService
import com.twitter.util._
import org.scalatest.matchers.ShouldMatchers
import scala.annotation.tailrec

class EmbeddedApp(
  app: App,
  clientFlags: Map[String, String] = Map(),
  resolverMap: Map[String, String] = Map(),
  extraArgs: Seq[String] = Seq(),
  startServer: Boolean = false,
  waitForWarmup: Boolean = true,
  runAppMain: Boolean = false)
  extends ShouldMatchers
  with Logging {

  /* Fields */

  val appName = app.getClass.getName
  private val futurePool = FuturePool.unboundedPool

  //Mutable state
  private var started = false
  private var startupFailed = false
  protected[finatra] var closed = false
  private var _mainResult: Future[Unit] = _

  /* Constructor */

  if (startServer) {
    start()
  }

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
      if (isGuiceApp && !runAppMain) {
        guiceApp.autoRunAppMain = false
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
    val allArgs = combineArgs
    println("Starting " + appName + " with " + allArgs.mkString(" "))

    _mainResult = futurePool {
      try {
        app.main(allArgs)
      } catch {
        case e if !NonFatal.isNonFatal(e) =>
          println("Fatal exception in server startup.")
          e.printStackTrace()
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
    (guiceServerArgs ++ deciderArgs ++ extraArgs ++
      ResolverMapUtils.resolverMapStr(resolverMap)).toArray
  }

  /* Private */

  private lazy val waitForStartupRetryPolicy = RetryPolicyUtils.constantRetry[Boolean](
    start = 1.second,
    numRetries = 25,
    shouldRetry = {case Return(started) => !started})

  def waitForWarmupComplete() {
    val started = retry(waitForStartupRetryPolicy) {
      if (startupFailed) {
        fail("Server startup failed")
      }

      println("Waiting for warmup phases to complete...")
      guiceApp.postWarmedUp
    }

    if (!started.get()) {
      throw new Exception("App: %s failed to startup.".format(appName))
    }    
    logAppStartup()
  }
  
  protected def logAppStartup() = {
    banner("App warmup completed (" + appName + ")")
  }

  private def guiceServerArgs =
    if (isEnvironmentModuleLoaded)
      Seq(
        "-environment=prod")
    else
      Seq()

  private def deciderArgs = {
    if (isDeciderModuleLoaded)
      Seq(
        "-decider.environment=production",
        "-decider.base=decider.yml")
    else
      Seq()
  }

  //TODO: Hack: Internal specific and will not work for non-Guice TwitterServer's
  private def isDeciderModuleLoaded: Boolean = {
    isModuleLoaded("DeciderModule")
  }

  //TODO: Hack: Internal specific and will not work for non-Guice TwitterServer's
  private def isEnvironmentModuleLoaded: Boolean = {
    isModuleLoaded("EnvironmentModule")
  }

  private def isModuleLoaded(name: String): Boolean = {
    isGuiceApp &&
      (guiceApp.requiredModules.mkString contains name)
  }

  @tailrec
  private def retry[T](retryPolicy: RetryPolicy[Try[T]])(func: => T): Try[T] = {
    val result = Try(func)
    retryPolicy(result) match {
      case Some((sleepTime, nextPolicy)) =>
        Thread.sleep(sleepTime.inMillis)
        retry(nextPolicy)(func)
      case None =>
        result
    }
  }
}
