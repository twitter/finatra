package com.twitter.finatra.test

import com.fasterxml.jackson.databind.JsonNode
import com.google.common.net.{HttpHeaders, MediaType}
import com.google.inject.Stage
import com.twitter.finagle.http._
import com.twitter.finatra.json.{FinatraObjectMapper, JsonDiff}
import com.twitter.inject.server.PortUtils.ephemeralLoopback
import com.twitter.inject.server.Ports
import org.jboss.netty.handler.codec.http.{HttpMethod, HttpResponseStatus}

class EmbeddedHttpServer(
  twitterServer: Ports,
  clientFlags: Map[String, String] = Map(),
  extraArgs: Seq[String] = Seq(),
  waitForWarmup: Boolean = true,
  stage: Stage = Stage.DEVELOPMENT,
  useSocksProxy: Boolean = false,
  skipAppMain: Boolean = false,
  defaultRequestHeaders: Map[String, String] = Map(),
  defaultHttpSecure: Boolean = false,
  mapperOverride: Option[FinatraObjectMapper] = None,
  httpPortFlag: String = "http.port")
  extends com.twitter.inject.server.EmbeddedTwitterServer(
    twitterServer,
    clientFlags + (httpPortFlag -> ephemeralLoopback),
    extraArgs,
    waitForWarmup,
    stage,
    useSocksProxy,
    skipAppMain,
    defaultRequestHeaders) {

  protected lazy val httpClient = {
    start()
    createHttpClient(
      "httpClient",
      twitterServer.httpExternalPort.getOrElse(throw new Exception("External HTTP port not bound")))
  }

  protected lazy val httpsClient = {
    start()
    createHttpClient(
      "httpsClient",
      twitterServer.httpsExternalPort.getOrElse(throw new Exception("External HTTPs port not bound")),
      secure = true)
  }

  protected lazy val mapper = mapperOverride getOrElse injector.instance[FinatraObjectMapper]

  override protected def logAppStartup() {
    super.logAppStartup()
    println("ExternalHttp -> http://localhost:" + twitterServer.httpExternalPort)
  }

  lazy val httpExternalPort = twitterServer.httpExternalPort.get
  lazy val externalHttpHostAndPort = "localhost:" + httpExternalPort

  override def close() {
    if (!closed) {
      super.close()

      if (twitterServer.httpExternalPort.isDefined) {
        httpClient.close()
      }
      if (twitterServer.httpsExternalPort.isDefined) {
        httpsClient.close()
      }

      closed = true
    }
  }

  /* TODO: Extract HTTP methods into HttpClient */
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
    jsonAwareHttpExecute(request, addAcceptHeader(accept, headers), suppress, andExpect, withLocation, withBody, withJsonBody, withJsonBodyNormalizer, withErrors, routeToAdminServer, secure = secure.getOrElse(defaultHttpSecure))
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

    jsonAwareHttpExecute(request, addAcceptHeader(accept, headers), suppress, andExpect, withLocation, withBody, withJsonBody, withJsonBodyNormalizer, withErrors, routeToAdminServer, secure = secure.getOrElse(defaultHttpSecure))
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

    jsonAwareHttpExecute(request, addAcceptHeader(accept, headers), suppress, andExpect, withLocation, withBody, withJsonBody, withJsonBodyNormalizer, withErrors, routeToAdminServer, secure = secure.getOrElse(defaultHttpSecure))
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
    jsonAwareHttpExecute(
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
    jsonAwareHttpExecute(request, addAcceptHeader(accept, headers), suppress, andExpect, withLocation, withBody, withJsonBody, withJsonBodyNormalizer, withErrors, routeToAdminServer, secure = secure.getOrElse(defaultHttpSecure))
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
    jsonAwareHttpExecute(request, addAcceptHeader(accept, headers), suppress, andExpect, withLocation, withBody, withJsonBody, withJsonBodyNormalizer, withErrors, routeToAdminServer, secure = secure.getOrElse(defaultHttpSecure))
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
    jsonAwareHttpExecute(request, addAcceptHeader(accept, headers), suppress, andExpect, withLocation, withBody, withJsonBody, withJsonBodyNormalizer, withErrors, routeToAdminServer, secure = secure.getOrElse(defaultHttpSecure))
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

    jsonAwareHttpExecute(
      Request(request),
      routeToAdminServer = routeToAdminServer,
      andExpect = andExpect,
      withBody = withBody,
      withJsonBody = withJsonBody,
      secure = secure.getOrElse(defaultHttpSecure))
  }

  /* Private */

  private def jsonAwareHttpExecute(
    request: Request,
    headers: Map[String, String] = Map(),
    suppress: Boolean = false,
    andExpect: HttpResponseStatus = Status.Ok,
    withLocation: String = null,
    withBody: String = null,
    withJsonBody: String = null,
    withJsonBodyNormalizer: JsonNode => JsonNode = null,
    withErrors: Seq[String] = null, //TODO: Deprecate
    routeToAdminServer: Boolean = false,
    secure: Boolean): Response = {

    val client = chooseHttpClient(request.path, routeToAdminServer, secure)
    val response = httpExecute(client, request, headers, suppress, andExpect, withLocation, withBody)

    if (withJsonBody != null) {
      if (!withJsonBody.isEmpty)
        JsonDiff.jsonDiff(response.contentString, withJsonBody, withJsonBodyNormalizer, verbose = false)
      else
        response.contentString should equal("")
    }

    if (withErrors != null) {
      JsonDiff.jsonDiff(response.contentString, Map("errors" -> withErrors), withJsonBodyNormalizer)
    }

    response
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
