package com.twitter.finatra.test

import com.fasterxml.jackson.databind.JsonNode
import com.google.common.net.{HttpHeaders, MediaType}
import com.google.inject.Stage
import com.twitter.finagle.builder.ClientBuilder
import com.twitter.finagle.http._
import com.twitter.finagle.service.RetryPolicy
import com.twitter.finagle.{ChannelClosedException, Service}
import com.twitter.finatra.conversions.map._
import com.twitter.finatra.conversions.option._
import com.twitter.finatra.conversions.time._
import com.twitter.finatra.guice.GuiceApp
import com.twitter.finatra.json.{FinatraObjectMapper, JsonDiff}
import com.twitter.finatra.test.Banner._
import com.twitter.finatra.test.EmbeddedTwitterServer._
import com.twitter.finatra.twitterserver.TwitterServerPorts
import com.twitter.finatra.utils.{PortUtils, RetryPolicyUtils}
import com.twitter.util._
import java.net.{InetSocketAddress, URI}
import java.util.concurrent.TimeUnit._
import org.jboss.netty.handler.codec.http.{HttpMethod, HttpResponseStatus}
import org.joda.time.Duration

object EmbeddedTwitterServer {
  private def resolveClientFlags(useSocksProxy: Boolean, clientFlags: Map[String, String]) = {
    if (useSocksProxy) {
      clientFlags ++ Map(
        "com.twitter.server.resolverZkHosts" -> "localhost:2181",
        "com.twitter.finagle.socks.socksProxyHost" -> "localhost",
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
 * @param resolverMap Resolver map entries (helper for creating the resolverMap clientFlag)
 * @param extraArgs Extra command line arguments
 * @param waitForWarmup Once the app is started, wait for App warmup to be completed
 * @param defaultRequestHeaders HTTP headers set for all HTTP requests (individual headers can be overridden in request methods e.g. httpGet(headers = Map(...)))
 * @param defaultHttpSecure Default HTTPS mode (can be overridden in request methods e.g. httpGet(secure = true/false))
 * @param mapper Override the default json mapper (used in HTTP json method e.g. httpGetJson)
 * @param stage Guice Stage used to create the server's injector. Since EmbeddedTwitterServer is used for testing, we default to Stage.DEVELOPMENT.
 *              This makes it possible to only mock objects that are used in a given test, at the expense of not checking that the entire
 *              object graph is valid. As such, you should always have at lease one Stage.PRODUCTION test for your service (which eagerly
 *              creates all Guice classes at startup)
 * @param useSocksProxy Use a tunneled socks proxy for external service discovery/calls (useful for manually run external integration tests that connect to external services)
 * @param skipAppMain Skip the running of appMain when the app starts. You will need to manually call app.appMain() later in your test.
 */
case class EmbeddedTwitterServer(
  twitterServer: TwitterServerPorts,
  clientFlags: Map[String, String] = Map(),
  resolverMap: Map[String, String] = Map(),
  extraArgs: Seq[String] = Seq(),
  waitForWarmup: Boolean = true,
  defaultRequestHeaders: Map[String, String] = Map(),
  defaultHttpSecure: Boolean = false,
  mapper: FinatraObjectMapper = FinatraObjectMapper.create(),
  stage: Stage = Stage.DEVELOPMENT,
  useSocksProxy: Boolean = false,
  skipAppMain: Boolean = false)
  extends EmbeddedApp(
    app = twitterServer,
    clientFlags = resolveClientFlags(useSocksProxy, clientFlags),
    resolverMap = resolverMap,
    extraArgs = extraArgs,
    waitForWarmup = waitForWarmup,
    skipAppMain = skipAppMain,
    stage = stage) {

  /* Protected */

  protected lazy val httpClient = createHttpClient(
    "httpClient",
    twitterServer.httpExternalPort.getOrElse(throw new Exception("External HTTP port not bound")))

  protected lazy val httpsClient = createHttpClient(
    "httpsClient",
    twitterServer.httpsExternalPort.getOrElse(throw new Exception("External HTTPs port not bound")),
    secure = true)

  protected lazy val httpAdminClient = createHttpClient(
    "httpAdminClient",
    twitterServer.httpAdminPort)


  override protected def nonGuiceWarmupComplete(): Boolean = {
    twitterServer.httpAdminPort != 0
  }

  override protected def logAppStartup() {
    banner(
      "Server Started (" + appName + ")" +
        "\nAdminHttp    -> http://localhost:%s/admin".format(twitterServer.httpAdminPort) +
        twitterServer.httpExternalPort.format("\nExternalHttp -> http://localhost:%s") +
        twitterServer.thriftPort.format("\nThriftPort   -> %d"))
  }

  /* Public */

  lazy val isGuiceTwitterServer = twitterServer.isInstanceOf[GuiceApp]
  lazy val adminHostAndPort = "localhost:" + twitterServer.httpAdminPort
  lazy val httpExternalPort = twitterServer.httpExternalPort.get
  lazy val externalHttpHostAndPort = "localhost:" + httpExternalPort
  lazy val thriftHostAndPort = "localhost:" + twitterServer.thriftPort.get

  override def close() {
    if (!closed) {
      super.close()

      httpAdminClient.close()
      if (twitterServer.httpExternalPort.isDefined) {
        httpClient.close()
      }
      if (twitterServer.httpsExternalPort.isDefined) {
        httpsClient.close()
      }

      closed = true
    }
  }

  def assertHealthy(healthy: Boolean = true) {
    start()
    assert(twitterServer.httpAdminPort != 0)
    val expectedBody = if(healthy) "OK\n" else ""

    httpGet(
      "/health",
      routeToAdminServer = true,
      andExpect = Status.Ok,
      withBody = expectedBody)
  }

  def httpGet(
    path: String,
    accept: MediaType = null,
    headers: Map[String, String] = Map(),
    suppress: Boolean = false,
    andExpect: HttpResponseStatus = Status.Ok,
    withLocation: String = null,
    withBody: String = null,
    withJsonBody: String = null,
    withJsonBodyNormalizer: JsonNode => JsonNode = null,
    withErrors: Seq[String] = null,
    routeToAdminServer: Boolean = false,
    secure: Option[Boolean] = None): Response = {

    val request = createApiRequest(path, HttpMethod.GET)
    httpExecute(request, addAcceptHeader(accept, headers), suppress, andExpect, withLocation, withBody, withJsonBody, withJsonBodyNormalizer, withErrors, routeToAdminServer, secure = secure.getOrElse(defaultHttpSecure))
  }

  def httpGetJson[T: Manifest](
    path: String,
    accept: MediaType = null,
    headers: Map[String, String] = Map(),
    suppress: Boolean = false,
    andExpect: HttpResponseStatus = Status.Ok,
    withLocation: String = null,
    withBody: String = null,
    withJsonBody: String = null,
    withJsonBodyNormalizer: JsonNode => JsonNode = null,
    normalizeJsonParsedReturnValue: Boolean = true,
    withErrors: Seq[String] = null,
    routeToAdminServer: Boolean = false,
    secure: Option[Boolean] = None): T = {

    val response =
      httpGet(path, accept = MediaType.JSON_UTF_8, headers = headers, suppress = suppress,
        andExpect = andExpect, withLocation = withLocation,
        withJsonBody = withJsonBody, withJsonBodyNormalizer = withJsonBodyNormalizer)

    jsonParseWithNormalizer(response, withJsonBodyNormalizer, normalizeJsonParsedReturnValue)
  }

  def httpPost(
    path: String,
    postBody: String,
    accept: MediaType = null,
    suppress: Boolean = false,
    contentType: String = Message.ContentTypeJson,
    headers: Map[String, String] = Map(),
    andExpect: HttpResponseStatus = null,
    withLocation: String = null,
    withBody: String = null,
    withJsonBody: String = null,
    withJsonBodyNormalizer: JsonNode => JsonNode = null,
    withErrors: Seq[String] = null,
    routeToAdminServer: Boolean = false,
    secure: Option[Boolean] = None): Response = {

    val request = createApiRequest(path, Method.Post)
    request.setContentString(postBody)
    request.headers.set(HttpHeaders.CONTENT_LENGTH, postBody.length)
    request.headers.set(HttpHeaders.CONTENT_TYPE, contentType)

    httpExecute(request, addAcceptHeader(accept, headers), suppress, andExpect, withLocation, withBody, withJsonBody, withJsonBodyNormalizer, withErrors, routeToAdminServer, secure = secure.getOrElse(defaultHttpSecure))
  }

  def httpPostJson[ResponseType: Manifest](
    path: String,
    postBody: String,
    suppress: Boolean = false,
    headers: Map[String, String] = Map(),
    andExpect: HttpResponseStatus = Status.Ok,
    withLocation: String = null,
    withBody: String = null,
    withJsonBody: String = null,
    withJsonBodyNormalizer: JsonNode => JsonNode = null,
    normalizeJsonParsedReturnValue: Boolean = false,
    withErrors: Seq[String] = null,
    routeToAdminServer: Boolean = false,
    secure: Option[Boolean] = None): ResponseType = {

    val response = httpPost(path, postBody, MediaType.JSON_UTF_8, suppress, Message.ContentTypeJson, headers, andExpect, withLocation, withBody, withJsonBody, withJsonBodyNormalizer, withErrors, routeToAdminServer, secure)
    jsonParseWithNormalizer(response, withJsonBodyNormalizer, normalizeJsonParsedReturnValue)
  }

  def httpPut(
    path: String,
    putBody: String,
    accept: MediaType = null,
    suppress: Boolean = false,
    headers: Map[String, String] = Map(),
    andExpect: HttpResponseStatus = null,
    withLocation: String = null,
    withBody: String = null,
    withJsonBody: String = null,
    withJsonBodyNormalizer: JsonNode => JsonNode = null,
    withErrors: Seq[String] = null,
    routeToAdminServer: Boolean = false,
    secure: Option[Boolean] = None): Response = {

    val request = createApiRequest(path, Method.Put)
    request.setContentString(putBody)
    request.headers().set(HttpHeaders.CONTENT_LENGTH, putBody.length)

    httpExecute(request, addAcceptHeader(accept, headers), suppress, andExpect, withLocation, withBody, withJsonBody, withJsonBodyNormalizer, withErrors, routeToAdminServer, secure = secure.getOrElse(defaultHttpSecure))
  }

  def httpPutJson[ResponseType: Manifest](
    path: String,
    putBody: String,
    suppress: Boolean = false,
    headers: Map[String, String] = Map(),
    andExpect: HttpResponseStatus = Status.Ok,
    withLocation: String = null,
    withBody: String = null,
    withJsonBody: String = null,
    withJsonBodyNormalizer: JsonNode => JsonNode = null,
    normalizeJsonParsedReturnValue: Boolean = false,
    withErrors: Seq[String] = null,
    routeToAdminServer: Boolean = false,
    secure: Option[Boolean] = None): ResponseType = {

    val response = httpPut(path, putBody, MediaType.JSON_UTF_8, suppress, headers, andExpect, withLocation, withBody, withJsonBody, withJsonBodyNormalizer, withErrors, routeToAdminServer, secure)
    jsonParseWithNormalizer(response, withJsonBodyNormalizer, normalizeJsonParsedReturnValue)
  }

  def httpDelete(
    path: String,
    deleteBody: String = null,
    accept: MediaType = null,
    suppress: Boolean = false,
    headers: Map[String, String] = Map(),
    andExpect: HttpResponseStatus = null,
    withLocation: String = null,
    withBody: String = null,
    withJsonBody: String = null,
    withJsonBodyNormalizer: JsonNode => JsonNode = null,
    withErrors: Seq[String] = null,
    routeToAdminServer: Boolean = false,
    secure: Option[Boolean] = None): Response = {

    val request = createApiRequest(path, Method.Delete)
    if (deleteBody != null) {
      request.setContentString(deleteBody)
    }
    httpExecute(
      request,
      addAcceptHeader(accept, headers),
      suppress,
      andExpect,
      withLocation,
      withBody,
      withJsonBody,
      withJsonBodyNormalizer,
      withErrors,
      routeToAdminServer,
      secure = secure.getOrElse(defaultHttpSecure))
  }

  def httpOptions(
    path: String,
    accept: MediaType = null,
    headers: Map[String, String] = Map(),
    suppress: Boolean = false,
    andExpect: HttpResponseStatus = Status.Ok,
    withLocation: String = null,
    withBody: String = null,
    withJsonBody: String = null,
    withJsonBodyNormalizer: JsonNode => JsonNode = null,
    withErrors: Seq[String] = null,
    routeToAdminServer: Boolean = false,
    secure: Option[Boolean] = None): Response = {

    val request = createApiRequest(path, HttpMethod.OPTIONS)
    httpExecute(request, addAcceptHeader(accept, headers), suppress, andExpect, withLocation, withBody, withJsonBody, withJsonBodyNormalizer, withErrors, routeToAdminServer, secure = secure.getOrElse(defaultHttpSecure))
  }

  def httpPatch(
    path: String,
    accept: MediaType = null,
    headers: Map[String, String] = Map(),
    suppress: Boolean = false,
    andExpect: HttpResponseStatus = Status.Ok,
    withLocation: String = null,
    withBody: String = null,
    withJsonBody: String = null,
    withJsonBodyNormalizer: JsonNode => JsonNode = null,
    withErrors: Seq[String] = null,
    routeToAdminServer: Boolean = false,
    secure: Option[Boolean] = None): Response = {

    val request = createApiRequest(path, HttpMethod.PATCH)
    httpExecute(request, addAcceptHeader(accept, headers), suppress, andExpect, withLocation, withBody, withJsonBody, withJsonBodyNormalizer, withErrors, routeToAdminServer, secure = secure.getOrElse(defaultHttpSecure))
  }

  def httpHead(
    path: String,
    accept: MediaType = null,
    headers: Map[String, String] = Map(),
    suppress: Boolean = false,
    andExpect: HttpResponseStatus = Status.Ok,
    withLocation: String = null,
    withBody: String = null,
    withJsonBody: String = null,
    withJsonBodyNormalizer: JsonNode => JsonNode = null,
    withErrors: Seq[String] = null,
    routeToAdminServer: Boolean = false,
    secure: Option[Boolean] = None): Response = {

    val request = createApiRequest(path, HttpMethod.HEAD)
    httpExecute(request, addAcceptHeader(accept, headers), suppress, andExpect, withLocation, withBody, withJsonBody, withJsonBodyNormalizer, withErrors, routeToAdminServer, secure = secure.getOrElse(defaultHttpSecure))
  }

  def httpFormPost(
    path: String,
    params: Map[String, String],
    multipart: Boolean = false,
    routeToAdminServer: Boolean = false,
    headers: Map[String, String] = Map.empty,
    andExpect: HttpResponseStatus = Status.Ok,
    withBody: String = null,
    withJsonBody: String = null,
    secure: Option[Boolean] = None): Response = {

    val request = RequestBuilder().
      url(normalizeURL(path)).
      addHeaders(headers).
      add(paramsToElements(params)).
      buildFormPost(multipart = multipart)

    httpExecute(
      Request(request),
      routeToAdminServer = routeToAdminServer,
      andExpect = andExpect,
      withBody = withBody,
      withJsonBody = withJsonBody,
      secure = secure.getOrElse(defaultHttpSecure))
  }

  /* Protected */

  override protected def combineArgs() = {
    adminAndLogArgs ++ serviceArgs ++ super.combineArgs
  }

  protected def createHttpClient(
    name: String,
    port: Int,
    tcpConnectTimeout: Duration = 60.seconds,
    connectTimeout: Duration = 60.seconds,
    requestTimeout: Duration = 300.seconds,
    retryPolicy: RetryPolicy[Try[Any]] = httpRetryPolicy,
    secure: Boolean = false): Service[Request, Response] = {

    val builder = ClientBuilder()
      .name(name)
      .codec(RichHttp[Request](Http()))
      .tcpConnectTimeout(tcpConnectTimeout.toTwitterDuration)
      .connectTimeout(connectTimeout.toTwitterDuration)
      .requestTimeout(requestTimeout.toTwitterDuration)
      .hosts(new InetSocketAddress("localhost", port))
      .hostConnectionLimit(75)
      .retryPolicy(retryPolicy)
      .failFast(false)

    if (secure)
      builder.tlsWithoutValidation().build()
    else
      builder.build()
  }

  protected def httpRetryPolicy: RetryPolicy[Try[Any]] = {
    // go/jira/CSL-565 TwitterServer regularly closing connections
    val connectionClosedExceptions: PartialFunction[Try[Any], Boolean] = {
      case Throw(e: ChannelClosedException) =>
        println("Retrying ChannelClosedException")
        true
    }

    RetryPolicyUtils.constantRetry(
      start = 1.second,
      numRetries = 10,
      shouldRetry = connectionClosedExceptions)
  }

  /* Private */

  private def httpExecute(
    request: Request,
    headers: Map[String, String] = Map(),
    suppress: Boolean = false,
    andExpect: HttpResponseStatus = Status.Ok,
    withLocation: String = null,
    withBody: String = null,
    withJsonBody: String = null,
    withJsonBodyNormalizer: JsonNode => JsonNode = null,
    withErrors: Seq[String] = null,
    routeToAdminServer: Boolean = false,
    secure: Boolean): Response = {

    /* Start server if needed */
    start()

    /* Pre-Execute */
    if (!suppress) {
      val msg = "HTTP " + request.method + " " + request.uri
      val postMsg = if (request.method == Method.Post)
        "\n" + postBodyStr(request)
      else
        ""

      println("\n\n")
      println("=" * 120)
      println(msg + postMsg)
      println("-" * 120)
    }

    /* Execute */
    val client = chooseHttpClient(request.path, routeToAdminServer, secure = secure)
    val response = handleRequest(request, client = client, additionalHeaders = headers)

    /* Post-Execute */
    printResponse(response, suppress)
    printResponseBody(response, suppress)
    if (andExpect != null && response.status != andExpect) {
      response.status should equal(andExpect)
    }

    if (withBody != null) {
      response.contentString should equal(withBody)
    }

    if (withJsonBody != null) {
      if (!withJsonBody.isEmpty)
        JsonDiff.jsonDiff(response.contentString, withJsonBody, withJsonBodyNormalizer, verbose = false)
      else
        response.contentString should equal("")
    }

    if (withLocation != null) {
      response.location should equal(Some(withLocation))
    }

    if (withErrors != null) {
      JsonDiff.jsonDiff(response.contentString, Map("errors" -> withErrors), withJsonBodyNormalizer)
    }

    response
  }

  private def postBodyStr(request: Request) = {
    val bodyStr = request.contentString
    try {
      mapper.writePrettyString(bodyStr)
    } catch {
      case e: Exception =>
        bodyStr
    }
  }

  private def printResponse(response: Response, suppress: Boolean) {
    if (!suppress) {
      println("-" * 120)
      println("[Status]      " + response.status)
      response.headerMap foreach { case (k, v) =>
        println(s"[Header] $k: $v")
      }
    }
  }

  private def printResponseBody(response: Response, suppress: Boolean) {
    if (!suppress) {
      if (response.contentString.isEmpty) {
        println("<EmptyBody>")
      }
      else {
        try {
          val jsonNode = mapper.parse[JsonNode](response.contentString)
          if (jsonNode.isObject || jsonNode.isArray)
            println(
              mapper.writePrettyString(jsonNode))
          else
            println(response.contentString)
        } catch {
          case e: Throwable =>
            println(response.contentString)
        }
        println()
      }
    }
  }

  private def adminAndLogArgs = Array(
    s"-admin.port=${PortUtils.getLoopbackHostAddress}:0",
    "-log.level=INFO")

  private def serviceArgs = {
    servicePortFlagName match {
      case Some(name) =>
        Seq(s"-$name=${PortUtils.getLoopbackHostAddress}:0")
      case None =>
        Seq.empty
    }
  }

  private def servicePortFlagName: Option[String] = {
    twitterServer.flag.getAll(includeGlobal = false) map {_.name} find { name =>
      name == "http.port" || name == "service.port"
    }
  }

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

  private def createApiRequest(path: String, method: HttpMethod = Method.Get) = {
    val pathToUse = determinePath(path)
    createRequest(method, pathToUse)
  }

  private def createRequest(method: HttpMethod, pathToUse: String): Request = {
    val request = Request(method, pathToUse)
    request.headers.set("Host", "localhost.twitter.com")
    request
  }

  private def determinePath(path: String): String = {
    if (path.startsWith("http"))
      URI.create(path).getPath
    else
      path
  }

  private def handleRequest(request: Request, client: Service[Request, Response], additionalHeaders: Map[String, String] = Map()): Response = {
    // Don't overwrite request.headers set by RequestBuilder in httpFormPost.
    val defaultNewHeaders = defaultRequestHeaders filterNotKeys request.headerMap.contains
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

  private def normalizeURL(path: String) = {
    if (path.startsWith("http://"))
      path
    else
      "http://localhost:8080%s".format(path)
  }

  private def paramsToElements(params: Map[String, String]): Seq[SimpleElement] = {
    (params map { case (key, value) =>
      SimpleElement(key, value)
    }).toSeq
  }

  private def chooseHttpClient(path: String, forceAdmin: Boolean, secure: Boolean) = {
    if (path.startsWith("/admin") || forceAdmin)
      httpAdminClient
    else if (secure)
      httpsClient
    else
      httpClient
  }

  private def addAcceptHeader(accept: MediaType, headers: Map[String, String]): Map[String, String] = {
    if (accept != null)
      headers + (HttpHeaders.ACCEPT -> accept.toString)
    else
      headers
  }

  private def jsonParseWithNormalizer[T: Manifest](
    response: Response,
    normalizer: JsonNode => JsonNode,
    normalizeParsedJsonNode: Boolean) = {
    val jsonNode = {
      val parsedJsonNode = mapper.parse[JsonNode](response.contentString)

      if (normalizer != null && normalizeParsedJsonNode)
        normalizer(parsedJsonNode)
      else
        parsedJsonNode
    }

    try {
      mapper.parse[T](jsonNode)
    } catch {
      case e: Exception =>
        println(s"Json parsing error $e trying to parse response $response with body " + response.contentString)
        throw e
    }
  }
}
