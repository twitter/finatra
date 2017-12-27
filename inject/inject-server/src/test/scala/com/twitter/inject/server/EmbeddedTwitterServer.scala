package com.twitter.inject.server

import com.google.common.net.MediaType
import com.google.inject.Stage
import com.twitter.conversions.time._
import com.twitter.finagle.builder.ClientBuilder
import com.twitter.finagle.http._
import com.twitter.finagle.http.codec.HttpCodec
import com.twitter.finagle.service.Backoff._
import com.twitter.finagle.service.RetryPolicy
import com.twitter.finagle.service.RetryPolicy._
import com.twitter.finagle.stats.{InMemoryStatsReceiver, StatsReceiver}
import com.twitter.finagle.{ChannelClosedException, Http, Service}
import com.twitter.inject.PoolUtils
import com.twitter.inject.app.{InjectionServiceModule, InjectionServiceWithAnnotationModule, InjectionServiceWithNamedAnnotationModule, StartupTimeoutException}
import com.twitter.inject.conversions.map._
import com.twitter.inject.modules.InMemoryStatsReceiverModule
import com.twitter.inject.server.EmbeddedTwitterServer._
import com.twitter.inject.server.PortUtils._
import com.twitter.server.AdminHttpServer
import com.twitter.util.lint.{GlobalRules, Rule}
import com.twitter.util.{Await, Duration, Future, Stopwatch, Try}
import java.lang.annotation.Annotation
import java.net.{InetSocketAddress, URI}
import java.util.concurrent.TimeUnit._
import org.apache.commons.lang.reflect.FieldUtils
import org.scalatest.Matchers
import scala.collection.JavaConverters._
import scala.reflect.runtime.universe._
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
 */
class EmbeddedTwitterServer(
  twitterServer: com.twitter.server.TwitterServer,
  flags: => Map[String, String] = Map(),
  args: => Seq[String] = Seq(),
  waitForWarmup: Boolean = true,
  stage: Stage = Stage.DEVELOPMENT,
  useSocksProxy: Boolean = false,
  defaultRequestHeaders: Map[String, String] = Map(),
  streamResponse: Boolean = false,
  verbose: Boolean = false,
  disableTestLogging: Boolean = false,
  maxStartupTimeSeconds: Int = 60,
  failOnLintViolation: Boolean = false
) extends Matchers {

  /* Additional Constructors */

  def this(twitterServer: Ports, flags: java.util.Map[String, String], stage: Stage) = {
    this(twitterServer, flags = flags.asScala.toMap, stage = stage)
  }

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

  val name = twitterServer.name
  private val mainRunnerFuturePool = PoolUtils.newFixedPool("Embedded " + name)

  //Mutable state
  private var starting = false
  private var started = false
  protected[inject] var closed = false
  private var _mainResult: Future[Unit] = _

  // This needs to be volatile because it is set in mainRunnerFuturePool onFailure
  // which is a different thread than waitForServerStarted, where it's read.
  @volatile private var startupFailedThrowable: Option[Throwable] = None

  /* Lazy Fields */

  lazy val httpAdminClient = {
    start()
    createHttpClient("httpAdminClient", httpAdminPort)
  }

  lazy val isInjectable = twitterServer.isInstanceOf[TwitterServer]
  lazy val injectableServer = twitterServer.asInstanceOf[TwitterServer]
  lazy val injector = {
    start()
    injectableServer.injector
  }

  lazy val statsReceiver =
    if (isInjectable) injector.instance[StatsReceiver] else new InMemoryStatsReceiver
  lazy val inMemoryStatsReceiver = statsReceiver.asInstanceOf[InMemoryStatsReceiver]
  lazy val adminHostAndPort = PortUtils.loopbackAddressForPort(httpAdminPort)

  /* Public */

  /**
   * Bind an instance of type [T] to the object graph of the underlying server.
   * This will REPLACE any previously bound instance of the given type.
   *
   * @param instance - to bind instance.
   * @tparam T - type of the instance to bind.
   * @return this [[EmbeddedTwitterServer]].
   *
   * @see [[https://twitter.github.io/finatra/user-guide/testing/index.html#feature-tests Feature Tests]]
   */
  def bind[T: TypeTag](instance: T): EmbeddedTwitterServer = {
    bindInstance[T](instance)
    this
  }

  /**
   * Bind an instance of type [T] annotated with Annotation type [A] to the object
   * graph of the underlying server. This will REPLACE any previously bound instance of
   * the given type bound with the given annotation type.
   *
   * @param instance - to bind instance.
   * @tparam T - type of the instance to bind.
   * @tparam A - type of the Annotation used to bind the instance.
   * @return this [[EmbeddedTwitterServer]].
   *
   * @see [[https://twitter.github.io/finatra/user-guide/testing/index.html#feature-tests Feature Tests]]
   */
  def bind[T: TypeTag, A <: Annotation: TypeTag](instance: T): EmbeddedTwitterServer = {
    bindInstance[T, A](instance)
    this
  }

  /**
   * Bind an instance of type [T] annotated with the given Annotation value to the object
   * graph of the underlying server. This will REPLACE any previously bound instance of
   * the given type bound with the given annotation.
   *
   * @param annotation - [[java.lang.annotation.Annotation]] instance value
   * @param instance - to bind instance.
   * @tparam T - type of the instance to bind.
   * @return this [[EmbeddedTwitterServer]].
   *
   * @see [[https://twitter.github.io/finatra/user-guide/testing/index.html#feature-tests Feature Tests]]
   */
  def bind[T: TypeTag](annotation: Annotation, instance: T): EmbeddedTwitterServer = {
    bindInstance[T](annotation, instance)
    this
  }


  def mainResult: Future[Unit] = {
    start()
    if (_mainResult == null) {
      throw new Exception("Server needs to be started by calling EmbeddedTwitterServer#start()")
    } else {
      _mainResult
    }
  }

  def isStarted = started

  // NOTE: Start is called in various places to "lazily start the server" as needed
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

  def close(): Unit = {
    if (!closed) {
      infoBanner(s"Closing ${this.getClass.getSimpleName}: " + name)
      try {
        val adminClientCloseFuture = if (started) httpAdminClient.close() else Future.Done
        Await.all(adminClientCloseFuture, twitterServer.close())
        mainRunnerFuturePool.executor.shutdown()
      } catch {
        case NonFatal(e) =>
          info(s"Error while closing ${this.getClass.getSimpleName}: $e")
          e.printStackTrace()
      }
      closed = true
    }
  }

  /**
   * NOTE: We avoid using slf4j-api info logging so that we can differentiate the
   * underlying server logs from the testing framework logging without requiring a
   * test logging configuration to be loaded.
   *
   * @param str - the string message to log
   */
  def info(str: String): Unit = {
    if (!disableLogging) {
      println(str)
    }
  }

  def infoBanner(str: String): Unit = {
    info("\n")
    info("=" * 75)
    info(str)
    info("=" * 75)
  }

  def assertStarted(started: Boolean = true): Unit = {
    assert(isInjectable)
    start()
    injectableServer.started should be(started)
  }

  def assertHealthy(healthy: Boolean = true): Unit = {
    healthResponse(healthy).get()
  }

  def isHealthy: Boolean = {
    httpAdminPort != 0 &&
    healthResponse(shouldBeHealthy = true).isReturn
  }

  def httpAdminPort: Int = {
    getPort(twitterServer.adminBoundAddress)
  }

  def adminHttpServerRoutes: Seq[AdminHttpServer.Route] = {
    val allRoutesField = FieldUtils.getField(
      twitterServer.getClass,
      "com$twitter$server$AdminHttpServer$$allRoutes",
      true
    )
    allRoutesField.get(twitterServer).asInstanceOf[Seq[AdminHttpServer.Route]]
  }

  def clearStats(): Unit = {
    inMemoryStatsReceiver.counters.clear()
    inMemoryStatsReceiver.stats.clear()
    inMemoryStatsReceiver.gauges.clear()
  }

  def statsMap = inMemoryStatsReceiver.stats.iterator.toMap.mapKeys(keyStr).toSortedMap
  def countersMap = inMemoryStatsReceiver.counters.iterator.toMap.mapKeys(keyStr).toSortedMap
  def gaugeMap = inMemoryStatsReceiver.gauges.iterator.toMap.mapKeys(keyStr).toSortedMap

  def printStats(includeGauges: Boolean = true): Unit = {
    infoBanner(name + " Stats")
    for ((key, values) <- statsMap) {
      val avg = values.sum / values.size
      val valuesStr = values.mkString("[", ", ", "]")
      info(f"$key%-70s = $avg = $valuesStr")
    }

    info("\nCounters:")
    for ((key, value) <- countersMap) {
      info(f"$key%-70s = $value")
    }

    if (includeGauges) {
      info("\nGauges:")
      for ((key, value) <- inMemoryStatsReceiver.gauges.iterator.toMap
          .mapKeys(keyStr)
          .toSortedMap) {
        info(f"$key%-70s = ${value()}")
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

  def httpGetAdmin(
    path: String,
    accept: MediaType = null,
    headers: Map[String, String] = Map(),
    suppress: Boolean = false,
    andExpect: Status = Status.Ok,
    withLocation: String = null,
    withBody: String = null
  ): Response = {

    val request = createApiRequest(path, Method.Get)
    httpExecute(
      httpAdminClient,
      request,
      addAcceptHeader(accept, headers),
      suppress,
      andExpect,
      withLocation,
      withBody
    )
  }

  /* Protected */

  protected def httpExecute(
    client: Service[Request, Response],
    request: Request,
    headers: Map[String, String] = Map(),
    suppress: Boolean = false,
    andExpect: Status = Status.Ok,
    withLocation: String = null,
    withBody: String = null
  ): Response = {

    start()

    /* Pre - Execute */

    /* Don't overwrite request.headers potentially in given request */
    val defaultHeaders = defaultRequestHeaders filterKeys { !request.headerMap.contains(_) }
    addOrRemoveHeaders(request, defaultHeaders)
    // headers added last so they can overwrite "defaults"
    addOrRemoveHeaders(request, headers)

    printRequest(request, suppress)

    /* Execute */
    val response = handleRequest(request, client = client)

    /* Post - Execute */
    printResponseMetadata(response, suppress)
    printResponseBody(response, suppress)
    if (andExpect != null && response.status != andExpect) {
      assert(response.status == andExpect, receivedResponseStr(response))
    }

    if (withBody != null) {
      assert(response.contentString == withBody, receivedResponseStr(response))
    }

    if (withLocation != null) {
      assert(
        response.location.get.endsWith(withLocation),
        "\nDiffering Location\n\nExpected Location is: "
          + withLocation
          + " \nActual Location is: "
          + response.location.get
          + receivedResponseStr(response)
      )
    }

    response
  }

  protected def createHttpClient(
    name: String,
    port: Int,
    tcpConnectTimeout: Duration = 60.seconds,
    connectTimeout: Duration = 60.seconds,
    requestTimeout: Duration = 300.seconds,
    retryPolicy: RetryPolicy[Try[Any]] = httpRetryPolicy,
    secure: Boolean = false
  ): Service[Request, Response] = {

    val host = new InetSocketAddress(PortUtils.loopbackAddress, port)
    val builder = ClientBuilder()
      .name(s"$name:$port")
      .stack(Http.client.withStreaming(streamResponse))
      .tcpConnectTimeout(tcpConnectTimeout)
      .connectTimeout(connectTimeout)
      .requestTimeout(requestTimeout)
      .hosts(host)
      .hostConnectionLimit(75)
      .retryPolicy(retryPolicy)
      .reportTo(new InMemoryStatsReceiver)
      .failFast(false)
      .daemon(true)

    if (secure)
      builder.tlsWithoutValidation().build()
    else
      builder.build()
  }

  protected def httpRetryPolicy: RetryPolicy[Try[Any]] = {
    backoff(constant(1.second) take 15) {
      case Throw(e: ChannelClosedException) =>
        println("Retrying ChannelClosedException")
        true
    }
  }

  protected def prettyRequestBody(request: Request): String = {
    request.contentString
  }

  protected def printNonEmptyResponseBody(response: Response): Unit = {
    info(response.contentString + "\n")
  }

  protected def createApiRequest(path: String, method: Method = Method.Get): Request = {
    val pathToUse =
      if (path.startsWith("http"))
        URI.create(path).getPath
      else
        path

    Request(method, pathToUse)
  }

  protected def nonInjectableServerStarted(): Boolean = {
    isHealthy
  }

  protected def logStartup(): Unit = {
    infoBanner("Server Started: " + name)
    info(s"AdminHttp      -> http://$adminHostAndPort/admin")
  }

  protected def combineArgs(): Array[String] = {
    val flagsStr =
      flagsAsArgs(resolveFlags(useSocksProxy, flags))
    ("-admin.port=" + PortUtils.ephemeralLoopback) +: (args ++ flagsStr).toArray
  }

  protected[twitter] def bindInstance[T: TypeTag](instance: T): Unit = {
    addInjectionServiceFrameworkOverrideModule(new InjectionServiceModule(instance))
  }

  protected[twitter] def bindInstance[T: TypeTag, A <: Annotation: TypeTag](instance: T): Unit = {
    addInjectionServiceFrameworkOverrideModule(
      new InjectionServiceWithAnnotationModule[T, A](instance)
    )
  }

  protected[twitter] def bindInstance[T: TypeTag](annotation: Annotation, instance: T): Unit = {
    addInjectionServiceFrameworkOverrideModule(
      new InjectionServiceWithNamedAnnotationModule[T](annotation, instance)
    )
  }

  /* Private */

  private def addInjectionServiceFrameworkOverrideModule(module: com.google.inject.Module): Unit = {
    if (!isInjectable) {
      throw new IllegalStateException("Cannot call bind() with a non-injectable underlying server.")
    }
    injectableServer.addFrameworkOverrideModules(module)
  }

  private def disableLogging = {
    disableTestLogging || System.getProperties
      .keySet()
      .contains("com.twitter.inject.test.logging.disabled")
  }

  private def keyStr(keys: Seq[String]): String = {
    keys.mkString("/")
  }

  private def receivedResponseStr(response: Response): String = {
    "\n\nReceived Response:\n" + HttpCodec.encodeResponseToString(response)
  }

  private def handleRequest(request: Request, client: Service[Request, Response]): Response = {

    val futureResponse = client(request)
    val elapsed = Stopwatch.start()
    try {
      Await.result(futureResponse)
    } catch {
      case e: Throwable =>
        println(
          "ERROR in request: " + request + " " + e + " in " + elapsed().inUnit(MILLISECONDS) + " ms"
        )
        throw e
    }
  }

  private def printRequest(request: Request, suppress: Boolean): Unit = {
    if (!suppress) {
      val headers = request.headerMap.mkString("[Header]\t", "\n[Header]\t", "")

      val msg = "HTTP " + request.method + " " + request.uri + "\n" + headers

      if (request.contentString.isEmpty)
        infoBanner(msg)
      else
        infoBanner(msg + "\n" + prettyRequestBody(request))
    }
  }

  private def printResponseMetadata(response: Response, suppress: Boolean): Unit = {
    if (!suppress) {
      info("-" * 75)
      info("[Status]\t" + response.status)
      info(response.headerMap.mkString("[Header]\t", "\n[Header]\t", ""))
    }
  }

  private def printResponseBody(response: Response, suppress: Boolean): Unit = {
    if (!suppress) {
      if (response.isChunked) {
        //no-op
      } else if (response.contentString.isEmpty) {
        info("*EmptyBody*")
      } else {
        printNonEmptyResponseBody(response)
      }
    }
  }

  // Deletes request headers with null-values in map.
  private def addOrRemoveHeaders(request: Request, headers: Map[String, String]): Unit = {
    for ((key, value) <- headers) {
      if (value == null) {
        request.headerMap.remove(key)
      } else {
        request.headerMap.set(key, value)
      }
    }
  }

  private def addAcceptHeader(
    accept: MediaType,
    headers: Map[String, String]
  ): Map[String, String] = {
    if (accept != null)
      headers + (Fields.Accept -> accept.toString)
    else
      headers
  }

  private def healthResponse(shouldBeHealthy: Boolean = true): Try[Response] = {
    val expectedBody = if (shouldBeHealthy) "OK\n" else ""

    Try {
      httpGetAdmin("/health", andExpect = Status.Ok, withBody = expectedBody, suppress = !verbose)
    }
  }

  private def flagsAsArgs(flags: Map[String, String]): Iterable[String] = {
    flags.map { case (k, v) => "-" + k + "=" + v }
  }

  private def isSingletonObject(server: com.twitter.server.TwitterServer) = {
    import scala.reflect.runtime.currentMirror
    currentMirror.reflect(server).symbol.isModuleClass
  }

  private def runNonExitingMain(): Unit = {
    // we call distinct here b/c port flag args can potentially be added multiple times
    val allArgs = combineArgs().distinct
    info("\nStarting " + name + " with args: " + allArgs.mkString(" "))

    _mainResult = mainRunnerFuturePool {
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
    } onFailure { e =>
      //If we rethrow, the exception will be suppressed by the Future Pool's monitor. Instead we save off the exception and rethrow outside the pool
      startupFailedThrowable = Some(e)
    }
  }

  private def throwStartupFailedException(): Unit = {
    println(s"\nEmbedded server $name failed to startup: ${startupFailedThrowable.get.getMessage}")
    throw startupFailedThrowable.get
  }

  private def waitForServerStarted(): Unit = {
    for (i <- 1 to maxStartupTimeSeconds) {
      info("Waiting for warmup phases to complete...")

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
      info(s"Warning: $numIssues Linter $issueString Found!")
      failures.foreach { case (rule, issues) =>
        info(s"\t* Rule: ${rule.name} - ${rule.description}")
        issues.foreach(issue => info(s"\t - $issue"))
      }
      info("After addressing these issues, consider enabling failOnLintViolation mode to prevent future issues from reaching production.")
      if (failOnLintViolation) {
        val e = new Exception(s"failOnLintViolation is enabled and $numIssues Linter ${issueString.toLowerCase()} found.")
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
