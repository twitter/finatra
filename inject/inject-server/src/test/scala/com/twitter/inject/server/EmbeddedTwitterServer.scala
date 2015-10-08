package com.twitter.inject.server

import com.google.common.net.{HttpHeaders, MediaType}
import com.google.inject.Stage
import com.twitter.conversions.time._
import com.twitter.finagle.builder.ClientBuilder
import com.twitter.finagle.http._
import com.twitter.finagle.service.Backoff._
import com.twitter.finagle.service.RetryPolicy
import com.twitter.finagle.service.RetryPolicy._
import com.twitter.finagle.stats.{InMemoryStatsReceiver, NullStatsReceiver, StatsReceiver}
import com.twitter.finagle.{ChannelClosedException, Service}
import com.twitter.inject.app.{App, EmbeddedApp}
import com.twitter.inject.modules.InMemoryStatsReceiverModule
import com.twitter.inject.server.EmbeddedTwitterServer._
import com.twitter.util._
import java.net.{InetSocketAddress, URI}
import java.util.concurrent.TimeUnit._
import scala.collection.immutable.SortedMap

object EmbeddedTwitterServer {
  private def resolveClientFlags(useSocksProxy: Boolean, clientFlags: Map[String, String]) = {
    if (useSocksProxy) {
      clientFlags ++ Map(
        "com.twitter.server.resolverZkHosts" -> PortUtils.loopbackAddressForPort(2181),
        "com.twitter.finagle.socks.socksProxyHost" -> PortUtils.loopbackAddress,
        "com.twitter.finagle.socks.socksProxyPort" -> "50001")
    }
    else {
      clientFlags
    }
  }
}

/**
 * EmbeddedTwitterServer allows a twitter-server serving http or thrift endpoints to be started
 * locally (on ephemeral ports), and tested through it's http/thrift interfaces.
 *
 * Note: All initialization fields are lazy to aid running multiple tests inside Intellij at the same time
 * since Intellij "pre-constructs" ALL the tests before running each one.
 *
 * @param twitterServer The twitter server to be started locally for integration testing
 * @param clientFlags Command line flags (e.g. "foo"->"bar" is translated into -foo=bar)
 * @param extraArgs Extra command line arguments
 * @param waitForWarmup Once the app is started, wait for App warmup to be completed
 * @param stage Guice Stage used to create the server's injector. Since EmbeddedTwitterServer is used for testing, we default to Stage.DEVELOPMENT.
 *              This makes it possible to only mock objects that are used in a given test, at the expense of not checking that the entire
 *              object graph is valid. As such, you should always have at lease one Stage.PRODUCTION test for your service (which eagerly
 *              creates all Guice classes at startup)
 * @param useSocksProxy Use a tunneled socks proxy for external service discovery/calls (useful for manually run external integration tests that connect to external services)
 * @param skipAppMain Skip the running of appMain when the app starts. You will need to manually call app.appMain() later in your test.
 */
class EmbeddedTwitterServer(
  val twitterServer: Ports,
  clientFlags: Map[String, String] = Map(),
  extraArgs: Seq[String] = Seq(),
  waitForWarmup: Boolean = true,
  stage: Stage = Stage.DEVELOPMENT,
  useSocksProxy: Boolean = false,
  skipAppMain: Boolean = false,
  defaultRequestHeaders: Map[String, String] = Map(),
  streamResponse: Boolean = false,
  verbose: Boolean = true,
  maxStartupTimeSeconds: Int = 60)
  extends EmbeddedApp(
    app = twitterServer,
    clientFlags = resolveClientFlags(useSocksProxy, clientFlags),
    resolverMap = Map(),
    extraArgs = extraArgs,
    waitForWarmup = waitForWarmup,
    skipAppMain = skipAppMain,
    stage = stage,
    verbose = verbose,
    maxStartupTimeSeconds = maxStartupTimeSeconds) {

  /* Additional Constructors */
  def this(twitterServer: Ports) = {
    this(twitterServer, stage = Stage.PRODUCTION)
  }

  /* Main Constructor */

  // Add framework override modules
  if (isGuiceApp) {
    guiceApp.addFrameworkOverrideModules(InMemoryStatsReceiverModule)
  }

  /* Lazy Fields */

  lazy val httpAdminClient = {
    start()
    createHttpClient(
      "httpAdminClient",
      twitterServer.httpAdminPort)
  }

  lazy val statsReceiver = if (isGuiceApp) injector.instance[StatsReceiver] else new InMemoryStatsReceiver
  lazy val inMemoryStatsReceiver = statsReceiver.asInstanceOf[InMemoryStatsReceiver]
  lazy val adminHostAndPort = PortUtils.loopbackAddressForPort(twitterServer.httpAdminPort)
  lazy val isGuiceTwitterServer = twitterServer.isInstanceOf[App]

  /* Overrides */

  override protected def nonGuiceAppStarted(): Boolean = {
    twitterServer.httpAdminPort != 0
  }

  override protected def logAppStartup() {
    infoBanner("Server Started: " + appName)
    info(s"AdminHttp    -> http://$adminHostAndPort/admin")
  }

  override protected def updateClientFlags(map: Map[String, String]) = {
    if (!verbose)
      map + ("log.level" -> "WARNING")
    else
      map
  }

  override def close() {
    if (!closed) {
      super.close()
      closed = true
    }
  }

  override protected def combineArgs(): Array[String] = {
    ("-admin.port=" + PortUtils.ephemeralLoopback) +: super.combineArgs
  }

  /* Public */

  def thriftPort: Int = {
    start()
    twitterServer.thriftPort.get
  }

  def thriftHostAndPort: String = {
    PortUtils.loopbackAddressForPort(thriftPort)
  }

  def clearStats() = {
    inMemoryStatsReceiver.counters.clear()
    inMemoryStatsReceiver.stats.clear()
    inMemoryStatsReceiver.gauges.clear()
  }

  def printStats(includeGauges: Boolean = true) {

    def keyStr(keys: Seq[String]): String = {
      keys.mkString("/")
    }

    infoBanner(appName + " Stats")
    for ((key, values) <- inMemoryStatsReceiver.stats.iterator.toMap.mapKeys(keyStr).toSortedMap) {
      val avg = values.sum / values.size
      val valuesStr = values.mkString("[", ", ", "]")
      info(f"$key%-70s = $avg = $valuesStr")
    }

    info("\nCounters:")
    for ((key, value) <- inMemoryStatsReceiver.counters.iterator.toMap.mapKeys(keyStr).toSortedMap) {
      info(f"$key%-70s = $value")
    }

    if (includeGauges) {
      info("\nGauges:")
      for ((key, value) <- inMemoryStatsReceiver.gauges.iterator.toMap.mapKeys(keyStr).toSortedMap) {
        info(f"$key%-70s = ${value()}")
      }
    }
  }

  def assertHealthy(healthy: Boolean = true) {
    val expectedBody = if (healthy) "OK\n" else ""

    httpGetAdmin(
      "/health",
      andExpect = Status.Ok,
      withBody = expectedBody)
  }

  def assertAppStarted(started: Boolean = true) {
    assert(isGuiceApp)
    start()
    guiceApp.appStarted should be(started)
  }

  def httpGetAdmin(
    path: String,
    accept: MediaType = null,
    headers: Map[String, String] = Map(),
    suppress: Boolean = false,
    andExpect: Status = Status.Ok,
    withLocation: String = null,
    withBody: String = null): Response = {

    val request = createApiRequest(path, Method.Get)
    httpExecute(httpAdminClient, request, addAcceptHeader(accept, headers), suppress, andExpect, withLocation, withBody)
  }

  /* Protected */

  protected def httpExecute(
    client: Service[Request, Response],
    request: Request,
    headers: Map[String, String] = Map(),
    suppress: Boolean = false,
    andExpect: Status = Status.Ok,
    withLocation: String = null,
    withBody: String = null): Response = {

    start()

    /* Pre - Execute */
    printRequest(request, suppress)

    /* Execute */
    val response = handleRequest(request, client = client, additionalHeaders = headers)

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
      assert(response.location.get.endsWith(withLocation), "\nDiffering Location\n\nExpected Location is: "
        + withLocation
        + " \nActual Location is: "
        + response.location.get
        + receivedResponseStr(response))
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
    secure: Boolean = false): Service[Request, Response] = {

    val host = new InetSocketAddress(PortUtils.loopbackAddress, port)
    val builder = ClientBuilder()
      .name(name)
      .codec(Http(_streaming = streamResponse))
      .tcpConnectTimeout(tcpConnectTimeout)
      .connectTimeout(connectTimeout)
      .requestTimeout(requestTimeout)
      .hosts(host)
      .hostConnectionLimit(75)
      .retryPolicy(retryPolicy)
      .reportTo(NullStatsReceiver)
      .failFast(false)

    if (secure)
      builder.tlsWithoutValidation().build()
    else
      builder.build()
  }

  protected def httpRetryPolicy: RetryPolicy[Try[Any]] = {
    backoff(
      constant(1.second) take 15) {
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

  /* Private */

  private def receivedResponseStr(response: Response) = {
    "\n\nReceived Response:\n" + response.encodeString()
  }

  private def handleRequest(
    request: Request,
    client: Service[Request, Response],
    additionalHeaders: Map[String, String] = Map()): Response = {

    // Don't overwrite request.headers set by RequestBuilder in httpFormPost.
    val defaultNewHeaders = defaultRequestHeaders filterKeys { !request.headerMap.contains(_) }
    addOrRemoveHeaders(request, defaultNewHeaders)
    addOrRemoveHeaders(request, additionalHeaders) //additional headers get added second so they can overwrite defaults

    val futureResponse = client(request)
    val elapsed = Stopwatch.start()
    try {
      Await.result(futureResponse)
    } catch {
      case e: Throwable =>
        println("ERROR in request: " + request + " " + e + " in " + elapsed().inUnit(MILLISECONDS) + " ms")
        throw e
    }
  }

  private def printRequest(request: Request, suppress: Boolean) {
    if (!suppress) {
      val headers = request.headerMap.mkString(
        "[Header]\t",
        "\n[Header]\t",
        "")

      val msg = "HTTP " + request.method + " " + request.uri + "\n" + headers

      if (request.contentString.isEmpty)
        infoBanner(msg)
      else
        infoBanner(msg + "\n" + prettyRequestBody(request))
    }
  }

  private def printResponseMetadata(response: Response, suppress: Boolean) {
    if (!suppress) {
      info("-" * 75)
      info("[Status]\t" + response.status)
      info(response.headerMap.mkString(
        "[Header]\t",
        "\n[Header]\t",
        ""))
    }
  }

  private def printResponseBody(response: Response, suppress: Boolean) {
    if (!suppress) {
      if (response.isChunked) {
        //no-op
      }
      else if (response.contentString.isEmpty) {
        info("*EmptyBody*")
      }
      else {
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

  protected def createApiRequest(path: String, method: Method = Method.Get) = {
    val pathToUse = if (path.startsWith("http"))
      URI.create(path).getPath
    else
      path

    Request(method, pathToUse)
  }

  private def addAcceptHeader(
    accept: MediaType,
    headers: Map[String, String]): Map[String, String] = {
    if (accept != null)
      headers + (HttpHeaders.ACCEPT -> accept.toString)
    else
      headers
  }

  //TODO: AF-567: Create inject-utils
  implicit class RichMap[K, V](wrappedMap: Map[K, V]) {
    def mapKeys[T](func: K => T): Map[T, V] = {
      for ((k, v) <- wrappedMap) yield {
        func(k) -> v
      }
    }

    def toSortedMap(implicit ordering: Ordering[K]) = {
      SortedMap[K, V]() ++ wrappedMap
    }
  }
}
