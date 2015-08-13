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
import com.twitter.inject.app.Banner._
import com.twitter.inject.app.{App, Banner, EmbeddedApp}
import com.twitter.inject.modules.InMemoryStatsReceiverModule
import com.twitter.inject.server.EmbeddedTwitterServer._
import com.twitter.util._
import java.net.{InetSocketAddress, URI}
import java.util.concurrent.TimeUnit._
import org.jboss.netty.handler.codec.http.{HttpMethod, HttpResponseStatus}

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
  streamResponse: Boolean = false)
  extends EmbeddedApp(
    app = twitterServer,
    clientFlags = resolveClientFlags(useSocksProxy, clientFlags),
    resolverMap = Map(),
    extraArgs = extraArgs,
    waitForWarmup = waitForWarmup,
    skipAppMain = skipAppMain,
    stage = stage) {

  /* Constructor */

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

  def thriftPort: Int = {
    start()
    twitterServer.thriftPort.get
  }

  def thriftHostAndPort: String = {
    PortUtils.loopbackAddressForPort(thriftPort)
  }

  /* Protected */

  override protected def nonGuiceAppStarted(): Boolean = {
    twitterServer.httpAdminPort != 0
  }

  override protected def logAppStartup() {
    Banner.banner("Server Started: " + appName)
    println(s"AdminHttp    -> http://$adminHostAndPort/admin")
  }

  /* Public */

  lazy val isGuiceTwitterServer = twitterServer.isInstanceOf[App]

  override def close() {
    if (!closed) {
      super.close()
      closed = true
    }
  }

  def clearStats() = {
    inMemoryStatsReceiver.counters.clear()
    inMemoryStatsReceiver.stats.clear()
    inMemoryStatsReceiver.gauges.clear()
  }

  def printStats() {
    def prettyKeys(keys: Seq[String]): String = {
      keys.mkString("/")
    }

    banner(appName + " Stats")

    for ((keys, values) <- inMemoryStatsReceiver.stats.iterator.toSeq.sortBy {_._1.head}) {
      val avg = values.sum / values.size
      println(prettyKeys(keys) + "\t = Avg " + avg + " with values " + values.mkString(", "))
    }

    for ((keys, value) <- inMemoryStatsReceiver.counters.iterator.toSeq.sortBy {_._1.head}) {
      println(prettyKeys(keys) + "\t = " + value)
    }

    for ((keys, value) <- inMemoryStatsReceiver.gauges.iterator.toSeq.sortBy {_._1.head}) {
      println(prettyKeys(keys) + "\t = " + value)
    }
  }

  def assertHealthy(healthy: Boolean = true) {
    val expectedBody = if (healthy) "OK\n" else ""

    httpGetAdmin(
      "/health",
      andExpect = Status.Ok,
      withBody = expectedBody)
  }

  def httpGetAdmin(
    path: String,
    accept: MediaType = null,
    headers: Map[String, String] = Map(),
    suppress: Boolean = false,
    andExpect: HttpResponseStatus = Status.Ok,
    withLocation: String = null,
    withBody: String = null): Response = {

    start()
    val request = createApiRequest(path, HttpMethod.GET)
    httpExecute(httpAdminClient, request, addAcceptHeader(accept, headers), suppress, andExpect, withLocation, withBody)
  }

  override protected def combineArgs() = {
    adminAndLogArgs ++ super.combineArgs
  }

  protected def httpExecute(
    client: Service[Request, Response],
    request: Request,
    headers: Map[String, String] = Map(),
    suppress: Boolean = false,
    andExpect: HttpResponseStatus = Status.Ok,
    withLocation: String = null,
    withBody: String = null): Response = {

    /* Pre - Execute */
    printRequest(request, suppress)

    /* Execute */
    val response = handleRequest(request, client = client, additionalHeaders = headers)

    /* Post - Execute */
    printResponseMetadata(response, suppress)
    printResponseBody(response, suppress)
    if (andExpect != null && response.status != andExpect) {
      assert(response.status == andExpect, response.encodeString())
    }

    if (withBody != null) {
      assert(response.contentString == withBody, response.encodeString())
    }

    if (withLocation != null) {
      assert(response.location.get.endsWith(withLocation), "\nDiffering Location\n\nExpected Location is: "
        + withLocation
        + " \nActual Location is: "
        + response.location.get + "\n"
        + response.encodeString())
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
      .codec(RichHttp[Request](Http(), aggregateChunks = !streamResponse))
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

  private def handleRequest(request: Request, client: Service[Request, Response], additionalHeaders: Map[String, String] = Map()): Response = {
    // Don't overwrite request.headers set by RequestBuilder in httpFormPost.
    val defaultNewHeaders = defaultRequestHeaders filterKeys {!request.headerMap.contains(_)}
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

  /* Private */

  protected def httpRetryPolicy: RetryPolicy[Try[Any]] = {
    backoff(
      constant(1.second) take 15) {
      case Throw(e: ChannelClosedException) =>
        println("Retrying ChannelClosedException")
        true
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
        banner(msg)
      else
        banner(msg + "\n" + prettyRequestBody(request))
    }
  }

  protected def prettyRequestBody(request: Request): String = {
    request.contentString
  }

  private def printResponseMetadata(response: Response, suppress: Boolean) {
    if (!suppress) {
      println("-" * 75)
      println("[Status]\t" + response.status)
      println(response.headerMap.mkString(
        "[Header]\t",
        "\n[Header]\t",
        ""))
    }
  }

  private def printResponseBody(response: Response, suppress: Boolean) {
    if (!suppress) {
      if (response.isChunked()) {
        //no-op
      }
      else if (response.contentString.isEmpty) {
        println("*EmptyBody*")
      }
      else {
        printNonEmptyResponseBody(response)
      }
    }
  }

  protected def printNonEmptyResponseBody(response: Response): Unit = {
    println(response.contentString)
    println()
  }

  private def adminAndLogArgs = Array(
    "-admin.port=" + PortUtils.ephemeralLoopback,
    "-log.level=INFO")

  // Deletes request headers with null-values in map.
  private def addOrRemoveHeaders(request: Request, headers: Map[String, String]): Unit = {
    for ((key, value) <- headers) {
      if (value == null) {
        request.headers.remove(key)
      } else {
        request.headers.set(key, value)
      }
    }
  }

  protected def createApiRequest(path: String, method: HttpMethod = Method.Get) = {
    val pathToUse = if (path.startsWith("http"))
      URI.create(path).getPath
    else
      path

    Request(method, pathToUse)
  }

  private def addAcceptHeader(accept: MediaType, headers: Map[String, String]): Map[String, String] = {
    if (accept != null)
      headers + (HttpHeaders.ACCEPT -> accept.toString)
    else
      headers
  }
}
