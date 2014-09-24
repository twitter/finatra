package com.twitter.finatra.test

import com.fasterxml.jackson.databind.JsonNode
import com.google.common.net.{HttpHeaders, MediaType}
import com.twitter.finagle.builder.ClientBuilder
import com.twitter.finagle.http._
import com.twitter.finagle.{ChannelClosedException, Service}
import com.twitter.finatra.FinatraRawServer
import com.twitter.finatra.conversions.json._
import com.twitter.finatra.conversions.options._
import com.twitter.finatra.conversions.time._
import com.twitter.finatra.guice.GuiceApp
import com.twitter.finatra.json.{FinatraObjectMapper, JsonDiff}
import com.twitter.finatra.test.Banner._
import com.twitter.finatra.test.EmbeddedTwitterServer.requestNum
import com.twitter.finatra.twitterserver.TwitterServerPorts
import com.twitter.finatra.utils.RetryPolicyUtils
import com.twitter.server.handler.ReplyHandler
import com.twitter.util._
import java.net.{InetSocketAddress, URI}
import java.util.concurrent.TimeUnit._
import java.util.concurrent.atomic.AtomicInteger
import org.jboss.netty.handler.codec.http.{HttpMethod, HttpResponseStatus}
import org.joda.time.Duration

object EmbeddedTwitterServer {
  val requestNum = new AtomicInteger(1)
}

/**
 * EmbeddedTwitterServer allows a twitter-server serving http or thrift endpoints to be started
 * locally (on ephemeral ports), and tested through it's http/thrift interfaces.
 * When testing HTTP endpoints, defaultRequestHeaders can be set for all test requests.
 *
 * All initialization fields are lazy to aid running multiple tests inside Intellij at the same time
 * since Intellij "pre-constructs" ALL the tests before running each one.
 */
case class EmbeddedTwitterServer(
  twitterServer: TwitterServerPorts,
  clientFlags: Map[String, String] = Map(),
  resolverMap: Map[String, String] = Map(),
  extraArgs: Seq[String] = Seq(),
  waitForWarmup: Boolean = true,
  defaultRequestHeaders: Map[String, String] = Map(),
  defaultHttpSecure: Boolean = false,
  mapper: FinatraObjectMapper = FinatraObjectMapper.create())
  extends EmbeddedApp(
    app = twitterServer,
    clientFlags = clientFlags,
    resolverMap = resolverMap,
    extraArgs = extraArgs,
    waitForWarmup = waitForWarmup,
    skipAppMain = true) {

  /* Constructor */

  //workaround for singleton HttpMuxer (only helps with sequential (non-parallel) server startup)
  HttpMuxer.addHandler("/health", new ReplyHandler(""))

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
    twitterServer.httpAdminPort,
    requestTimeout = 120.seconds)

  override protected def logAppStartup {
    banner(
      "Server Started (" + appName + ")" +
        "\nAdminHttp    -> http://localhost:%s/admin".format(twitterServer.httpAdminPort) +
        twitterServer.httpExternalPort.format("\nExternalHttp -> http://localhost:%s") +
        twitterServer.thriftPort.format("\nThriftPort   -> %d"))
  }

  /* Public */

  lazy val isGuiceTwitterServer = twitterServer.isInstanceOf[GuiceApp]
  lazy val isFinatraRawServer = twitterServer.isInstanceOf[FinatraRawServer]
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

  def httpGet(
    path: String,
    accept: MediaType = null,
    headers: Map[String, String] = Map(),
    suppress: Boolean = false,
    andExpect: HttpResponseStatus = Status.Ok,
    withLocation: String = null,
    withBody: String = null,
    withJsonBody: String = null,
    withNormalizer: JsonNode => JsonNode = null,
    withErrors: Seq[String] = null,
    routeToAdminServer: Boolean = false,
    secure: Option[Boolean] = None): Response = {

    val request = createApiRequest(path, HttpMethod.GET)
    httpExecute(request, addAcceptHeader(accept, headers), suppress, andExpect, withLocation, withBody, withJsonBody, withNormalizer, withErrors, routeToAdminServer, secure = secure.getOrElse(defaultHttpSecure))
  }

  def httpGetJson[T: Manifest](
    url: String,
    suppress: Boolean = false,
    andExpect: HttpResponseStatus = Status.Ok,
    withJsonBody: String = null,
    withNormalizer: JsonNode => JsonNode = null): T = {

    val response = httpGet(url, accept = MediaType.JSON_UTF_8, suppress = suppress, andExpect = andExpect, withJsonBody = withJsonBody, withNormalizer = withNormalizer)
    val jsonStr = response.contentString
    try {
      mapper.parse[T](jsonStr)
    } catch {
      case e: Exception =>
        println("JsonParsingError " + response + " " + e.getMessage)
        println("HTTP GET Received: " + jsonStr)
        throw e
    }
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
    withNormalizer: JsonNode => JsonNode = null,
    withErrors: Seq[String] = null,
    routeToAdminServer: Boolean = false,
    secure: Option[Boolean] = None): Response = {

    val request = createApiRequest(path, Method.Post)
    request.setContentString(postBody)
    request.headers.set(HttpHeaders.CONTENT_LENGTH, postBody.length)
    request.headers.set(HttpHeaders.CONTENT_TYPE, contentType)

    httpExecute(request, addAcceptHeader(accept, headers), suppress, andExpect, withLocation, withBody, withJsonBody, withNormalizer, withErrors, routeToAdminServer, secure = secure.getOrElse(defaultHttpSecure))
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
    withNormalizer: JsonNode => JsonNode = null,
    withErrors: Seq[String] = null,
    routeToAdminServer: Boolean = false,
    secure: Option[Boolean] = None): ResponseType = {

    val response = httpPost(path, postBody, MediaType.JSON_UTF_8, suppress, Message.ContentTypeJson, headers, andExpect, withLocation, withBody, withJsonBody, withNormalizer, withErrors, routeToAdminServer, secure)
    mapper.parse[ResponseType](response.contentString)
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
    withNormalizer: JsonNode => JsonNode = null,
    withErrors: Seq[String] = null,
    routeToAdminServer: Boolean = false,
    secure: Option[Boolean] = None): Response = {

    val request = createApiRequest(path, Method.Put)
    request.setContentString(putBody)
    request.headers().set(HttpHeaders.CONTENT_LENGTH, putBody.length)

    httpExecute(request, addAcceptHeader(accept, headers), suppress, andExpect, withLocation, withBody, withJsonBody, withNormalizer, withErrors, routeToAdminServer, secure = secure.getOrElse(defaultHttpSecure))
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
    withNormalizer: JsonNode => JsonNode = null,
    withErrors: Seq[String] = null,
    routeToAdminServer: Boolean = false,
    secure: Option[Boolean] = None): ResponseType = {

    val response = httpPut(path, putBody, MediaType.JSON_UTF_8, suppress, headers, andExpect, withLocation, withBody, withJsonBody, withNormalizer, withErrors, routeToAdminServer, secure)
    mapper.parse[ResponseType](response.contentString)
  }

  def httpDelete(
    path: String,
    accept: MediaType = null,
    suppress: Boolean = false,
    headers: Map[String, String] = Map(),
    andExpect: HttpResponseStatus = null,
    withLocation: String = null,
    withBody: String = null,
    withJsonBody: String = null,
    withNormalizer: JsonNode => JsonNode = null,
    withErrors: Seq[String] = null,
    routeToAdminServer: Boolean = false,
    secure: Option[Boolean] = None): Response = {

    val request = createApiRequest(path, Method.Delete)
    httpExecute(
      request,
      addAcceptHeader(accept, headers),
      suppress,
      andExpect,
      withLocation,
      withBody,
      withJsonBody,
      withNormalizer,
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
    withNormalizer: JsonNode => JsonNode = null,
    withErrors: Seq[String] = null,
    routeToAdminServer: Boolean = false,
    secure: Option[Boolean] = None): Response = {

    val request = createApiRequest(path, HttpMethod.OPTIONS)
    httpExecute(request, addAcceptHeader(accept, headers), suppress, andExpect, withLocation, withBody, withJsonBody, withNormalizer, withErrors, routeToAdminServer, secure = secure.getOrElse(defaultHttpSecure))
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
    withNormalizer: JsonNode => JsonNode = null,
    withErrors: Seq[String] = null,
    routeToAdminServer: Boolean = false,
    secure: Option[Boolean] = None): Response = {

    val request = createApiRequest(path, HttpMethod.PATCH)
    httpExecute(request, addAcceptHeader(accept, headers), suppress, andExpect, withLocation, withBody, withJsonBody, withNormalizer, withErrors, routeToAdminServer, secure = secure.getOrElse(defaultHttpSecure))
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
    withNormalizer: JsonNode => JsonNode = null,
    withErrors: Seq[String] = null,
    routeToAdminServer: Boolean = false,
    secure: Option[Boolean] = None): Response = {

    val request = createApiRequest(path, HttpMethod.HEAD)
    httpExecute(request, addAcceptHeader(accept, headers), suppress, andExpect, withLocation, withBody, withJsonBody, withNormalizer, withErrors, routeToAdminServer, secure = secure.getOrElse(defaultHttpSecure))
  }

  def httpFormPost(
    path: String,
    params: Map[String, String],
    multipart: Boolean,
    routeToAdminServer: Boolean = false,
    headers: Seq[(String, String)] = Seq(),
    andExpect: HttpResponseStatus = Status.Ok,
    withBody: String = null,
    withJsonBody: String = null,
    secure: Option[Boolean] = None): Response = {

    val request = RequestBuilder().
      url(normalizeURL(path)).
      addHeaders(headers.toMap).
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

  /* Private */

  private def httpExecute(
    request: Request,
    headers: Map[String, String] = Map(),
    suppress: Boolean = false,
    andExpect: HttpResponseStatus = Status.Ok,
    withLocation: String = null,
    withBody: String = null,
    withJsonBody: String = null,
    withNormalizer: JsonNode => JsonNode = null,
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
        JsonDiff.jsonDiff(response.contentString, withJsonBody, withNormalizer)
      else
        response.contentString should equal("")
    }

    if (withLocation != null) {
      response.location should equal(Some(withLocation))
    }

    if (withErrors != null) {
      JsonDiff.jsonDiff(response.contentString, Map("errors" -> withErrors), withNormalizer)
    }

    response
  }

  private def postBodyStr(request: Request) = {
    val bodyStr = request.contentString
    try {
      bodyStr.toPrettyJson
    } catch {
      case e: Exception =>
        bodyStr
    }
  }

  private def printResponse(response: Response, suppress: Boolean) {
    if (!suppress) {
      println("-" * 120)
      println("Status      " + response.status)
      println("ContentType " + Option(response.headers.get("Content-Type")).getOrElse("<Empty>"))
      val location = response.headers.get("Location")
      if (location != null) {
        println("Location    " + location)
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
            println(jsonNode.toPrettyJson)
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

  /* Make sure health returns OK three times in a row */
  def isHealthy: Boolean = {
    start()

    twitterServer.httpAdminPort != 0 &&
      healthOk() &&
      healthOk() &&
      healthOk()
  }

  private def healthOk(): Boolean = {
    Thread.sleep(50)
    try {
      val response = httpGet(
        "/health",
        routeToAdminServer = true,
        andExpect = Status.Ok)

      response.contentString == "OK\n"
    }
    catch {
      case NonFatal(e) =>
        error("Error checking health of server " + appName, e)
        throw e
    }
  }

  def standardArgs = Array(
    "-admin.port=:0",
    "-log.level=INFO")

  def httpArgs = {
    if (isHttpServer)
      Seq("-http.port=:0")
    else
      Seq()
  }

  private def isHttpServer = {
    isFinatraRawServer
  }

  override def combineArgs() = {
    standardArgs ++ httpArgs ++ super.combineArgs
  }

  private def addHeaders(request: Request, headers: Map[String, String]) {
    for ((key, value) <- headers) {
      request.headers.set(key, value)
    }
  }

  private def createApiRequest(path: String, method: HttpMethod = Method.Get) = {
    val pathToUse = determinePath(path)
    createRequest(method, pathToUse)
  }

  def createRequest(method: HttpMethod, pathToUse: String): Request = {
    val request = Request(method, pathToUse)
    request.headers.set("Host", "localhost.twitter.com")
***REMOVED***
    request
  }

  private def determinePath(path: String): String = {
    if (path.startsWith("http"))
      URI.create(path).getPath
    else
      path
  }

  private def handleRequest(request: Request, client: Service[Request, Response], additionalHeaders: Map[String, String] = Map()): Response = {
    addHeaders(request, defaultRequestHeaders)
    addHeaders(request, additionalHeaders) //additional headers get added second so they can overwrite defaults
    val futureResponse = client(request)
    val elapsed = Stopwatch.start()
    try {
      Await.result(futureResponse, 30000.seconds.toTwitterDuration)
    } catch {
      case e: Throwable =>
        println("ERROR in request: " + request + " " + e + " in " + elapsed().inUnit(MILLISECONDS) + " ms")
        throw e
    }
  }

  private def createHttpClient(name: String, port: Int, requestTimeout: Duration = 120.seconds, secure: Boolean = false): Service[Request, Response] = {
    // go/jira/CSL-565 TwitterServer regularly closing connections
    val connectionClosedExceptions: PartialFunction[Try[Any], Boolean] = {
      case Throw(e: ChannelClosedException) =>
        println("Retrying ChannelClosedException")
        true
    }

    val retryPolicy = RetryPolicyUtils.constantRetry(
      start = 1.second,
      numRetries = 10,
      shouldRetry = connectionClosedExceptions)

    val builder = ClientBuilder()
      .name(name)
      .codec(RichHttp[Request](Http()))
      .tcpConnectTimeout(30.seconds.toTwitterDuration)
      .connectTimeout(30.seconds.toTwitterDuration)
      .requestTimeout(requestTimeout.toTwitterDuration)
      .hosts(new InetSocketAddress("localhost", port))
      .hostConnectionLimit(75)
      .retryPolicy(retryPolicy)

    if (secure)
      builder.tlsWithoutValidation().build()
    else
      builder.build()
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
}
