package com.twitter.finatra.http

import com.fasterxml.jackson.databind.JsonNode
import com.google.inject.Stage
import com.twitter.app.GlobalFlag
import com.twitter.finagle.http.{MediaType, Method, Status, _}
import com.twitter.finagle.stats.StatsReceiver
import com.twitter.finatra.http.JsonAwareEmbeddedHttpClient.jsonParseWithNormalizer
import com.twitter.finatra.http.routing.HttpRouter
import com.twitter.finatra.jackson.ScalaObjectMapper
import com.twitter.inject.conversions.map._
import com.twitter.inject.server.{EmbeddedHttpClient, EmbeddedTwitterServer, Ports}
import com.twitter.util.{Duration, Memoize}
import java.lang.annotation.Annotation
import scala.collection.JavaConverters._

/**
 *
 * EmbeddedHttpServer allows a [[com.twitter.server.TwitterServer]] serving http endpoints to be started
 * locally (on ephemeral ports), and tested through it's http interfaces.
 *
 * @param twitterServer                     The [[com.twitter.server.TwitterServer]] to be started for testing.
 * @param flags Command line flags (e.g. "foo"->"bar" is translated into -foo=bar). See: [[com.twitter.app.Flag]].
 * @param args Extra command line arguments.
 * @param waitForWarmup Once the server is started, wait for server warmup to be completed
 * @param stage                    [[com.google.inject.Stage]] used to create the server's injector. Since EmbeddedHttpServer is used for testing,
 *                                          we default to Stage.DEVELOPMENT. This makes it possible to only mock objects that are used in a given test,
 *                                          at the expense of not checking that the entire object graph is valid. As such, you should always have at
 *                                          least one Stage.PRODUCTION test for your service (which eagerly creates all classes at startup)
 * @param useSocksProxy                     Use a tunneled socks proxy for external service discovery/calls (useful for manually running external
 *                                          integration tests that connect to external services).
 * @param defaultRequestHeaders             Headers to always send to the embedded server.
 * @param defaultHttpSecure                 Default all requests to the server to be HTTPS.
 * @param mapperOverride                    [[com.twitter.finatra.jackson.ScalaObjectMapper]] to use instead of the mapper configuered by
 *                                          the embedded server.
 * @param httpPortFlag                      Name of the flag that defines the external http port for the server.
 * @param streamResponse                    Toggle to not unwrap response content body to allow caller to stream response.
 * @param verbose                           Enable verbose logging during test runs.
 * @param disableTestLogging                Disable all logging emitted from the test infrastructure.
 * @param maxStartupTimeSeconds          Maximum seconds to wait for embedded server to start. If exceeded a
 *                                       [[com.twitter.inject.app.StartupTimeoutException]] is thrown.
 * @param failOnLintViolation If server startup should fail due (and thus the test) to a detected lint rule issue after startup.
 * @param closeGracePeriod An Optional grace period to use instead of the underlying server's
 *                         `defaultGracePeriod` when closing the underlying server.
 * @param globalFlags           An ordered map of [[GlobalFlag]] and the desired value to be set during the
 *                              scope of the underlying [[twitterServer]]'s lifecycle. The flags will be
 *                              applied in insertion order, with the first entry being applied closest to
 *                              the startup of the [[twitterServer]]. In order to ensure insertion ordering,
 *                              you should use a [[scala.collection.immutable.ListMap]].
 * @param statsReceiverOverride An optional [[StatsReceiver]] implementation that should be bound to the
 *                                          underlying server when testing with an injectable server. By default
 *                                          an injectable server under test will have an [[com.twitter.finagle.stats.InMemoryStatsReceiver]]
 *                                          implementation bound for the purpose of testing. In some cases, users may want to test using
 *                                          a custom [[StatsReceiver]] implementation instead and can provide an instance
 *                                          to use here. For non-injectable servers this can be a shared reference
 *                                          used in the server under test.
 */
class EmbeddedHttpServer(
  override val twitterServer: Ports,
  flags: => Map[String, String] = Map(),
  args: => Seq[String] = Seq(),
  waitForWarmup: Boolean = true,
  stage: Stage = Stage.DEVELOPMENT,
  useSocksProxy: Boolean = false,
  override val defaultRequestHeaders: Map[String, String] = Map(),
  defaultHttpSecure: Boolean = false,
  override val mapperOverride: Option[ScalaObjectMapper] = None,
  override val httpPortFlag: String = "http.port",
  override val streamResponse: Boolean = false,
  verbose: Boolean = false,
  disableTestLogging: Boolean = false,
  maxStartupTimeSeconds: Int = 60,
  failOnLintViolation: Boolean = false,
  closeGracePeriod: Option[Duration] = None,
  globalFlags: => Map[GlobalFlag[_], String] = Map(),
  statsReceiverOverride: Option[StatsReceiver] = None)
    extends EmbeddedTwitterServer(
      twitterServer = twitterServer,
      flags = flags,
      args = args,
      waitForWarmup = waitForWarmup,
      stage = stage,
      useSocksProxy = useSocksProxy,
      defaultRequestHeaders = defaultRequestHeaders,
      streamResponse = streamResponse,
      verbose = verbose,
      disableTestLogging = disableTestLogging,
      maxStartupTimeSeconds = maxStartupTimeSeconds,
      failOnLintViolation = failOnLintViolation,
      closeGracePeriod = closeGracePeriod,
      globalFlags = globalFlags,
      statsReceiverOverride = statsReceiverOverride
    )
    with ExternalHttpClient {

  /* Additional Constructors */

  def this(twitterServer: Ports, flags: java.util.Map[String, String], stage: Stage) =
    this(twitterServer, flags = flags.asScala.toMap, stage = stage)

  def this(
    twitterServer: Ports,
    flags: java.util.Map[String, String],
    globalFlags: java.util.Map[GlobalFlag[_], String],
    stage: Stage
  ) =
    this(
      twitterServer,
      flags = flags.asScala.toMap,
      stage = stage,
      globalFlags = globalFlags.toOrderedMap)

  /* Public */

  /**
   * Performs a GET request against the embedded server.
   *
   * @note Java users: see the more Java-friendly [[httpRequest(request: Request)]].
   *
   * @param path - URI of the request
   * @param accept - add request Accept header with the given [[com.twitter.finagle.http.MediaType]]
   * @param headers - additional headers that should be passed with the request
   * @param suppress - suppress http client logging
   * @param andExpect - expected [[com.twitter.finagle.http.Status]] value
   * @param withLocation - expected response Location header value
   * @param withBody - expected body as a String
   * @param withJsonBody - expected body as JSON
   * @param withJsonBodyNormalizer - normalizer to use in conjunction with withJsonBody
   * @param withErrors - expected errors
   * @param routeHint - optionally force the request to the main or admin interface of the embedded server, RouteHint.None by default
   * @param secure - use the https port to address the embedded server, default = None
   * @return a [[com.twitter.finagle.http.Response]] on success otherwise an exception
   *         if any of the assertions defined by andExpect or withXXXX fail
   */
  def httpGet(
    path: String,
    accept: String = null,
    headers: Map[String, String] = Map(),
    suppress: Boolean = false,
    andExpect: Status = Status.Ok,
    withLocation: String = null,
    withBody: String = null,
    withJsonBody: String = null,
    withJsonBodyNormalizer: JsonNode => JsonNode = null,
    withErrors: Seq[String] = null,
    routeHint: RouteHint = RouteHint.None,
    secure: Option[Boolean] = None
  ): Response = {

    val request = createApiRequest(path, Method.Get)
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
      routeHint,
      secure = secure.getOrElse(defaultHttpSecure)
    )
  }

  /**
   * Performs a GET request to the embedded server serializing the normalized
   * response#contentString into an instance of type [[ResponseType]].
   *
   * @note Java users: see the more Java-friendly [[httpRequest(request: Request)]].
   * @see [[com.twitter.finatra.jackson.ScalaObjectMapper]]#parse[T: Manifest](string: String)
   * @param path - URI of the request
   * @param accept - add request Accept header with the given [[com.twitter.finagle.http.MediaType]]
   * @param headers - additional headers that should be passed with the request
   * @param suppress - suppress http client logging
   * @param andExpect - expected [[com.twitter.finagle.http.Status]] value
   * @param withLocation - expected response Location header value
   * @param withBody - expected body as a String
   * @param withJsonBody - expected body as JSON
   * @param withJsonBodyNormalizer - normalizer to use in conjunction with withJsonBody.
   * @param normalizeJsonParsedReturnValue - if the normalizer SHOULD be applied on the parsing of the
   *                                       response#contentString into type [[ResponseType]], default = false.
   * @param withErrors - expected errors
   * @param routeHint - optionally force the request to the main or admin interface of the embedded server, RouteHint.None by default
   * @param secure - use the https port to address the embedded server, default = None
   * @tparam ResponseType - parse the response#contentString into type [[ResponseType]]
   *
   * @return instance of type [[ResponseType]] serialized from the the response#contentString.
   */
  def httpGetJson[ResponseType: Manifest](
    path: String,
    accept: String = null,
    headers: Map[String, String] = Map(),
    suppress: Boolean = false,
    andExpect: Status = Status.Ok,
    withLocation: String = null,
    withBody: String = null,
    withJsonBody: String = null,
    withJsonBodyNormalizer: JsonNode => JsonNode = null,
    normalizeJsonParsedReturnValue: Boolean = true,
    withErrors: Seq[String] = null,
    routeHint: RouteHint = RouteHint.None,
    secure: Option[Boolean] = None
  ): ResponseType = {

    assert(
      manifest[ResponseType] != manifest[Nothing],
      "httpGetJson requires a type-param to parse the JSON response into, e.g. http<Method>Json[MyCaseClass] or http<Method>Json[JsonNode]"
    )
    val response =
      httpGet(
        path,
        accept = MediaType.JsonUtf8,
        headers = headers,
        suppress = suppress,
        andExpect = andExpect,
        withLocation = withLocation,
        withJsonBody = withJsonBody,
        withJsonBodyNormalizer = withJsonBodyNormalizer,
        withErrors = withErrors,
        routeHint = routeHint,
        secure = secure
      )

    jsonParseWithNormalizer(
      response,
      normalizeJsonParsedReturnValue,
      mapper,
      withJsonBodyNormalizer
    )
  }

  /**
   * Performs a POST request to the embedded server.
   *
   * @note Java users: see the more Java-friendly [[httpRequest(request: Request)]].
   *
   * @param path - URI of the request
   * @param postBody - body of the POST request
   * @param accept - add request Accept header with the given [[com.twitter.finagle.http.MediaType]]
   * @param suppress - suppress http client logging
   * @param contentType - request Content-Type header value, application/json by default
   * @param headers - additional headers that should be passed with the request
   * @param andExpect - expected [[com.twitter.finagle.http.Status]] value
   * @param withLocation - expected response Location header value
   * @param withBody - expected body as a String
   * @param withJsonBody - expected body as JSON
   * @param withJsonBodyNormalizer - normalizer to use in conjunction with withJsonBody
   * @param withErrors - expected errors
   * @param routeHint - optionally force the request to the main or admin interface of the embedded server, RouteHint.None by default
   * @param secure - use the https port to address the embedded server, default = None
   * @return a [[com.twitter.finagle.http.Response]] on success otherwise an exception
   *         if any of the assertions defined by andExpect or withXXXX fail
   */
  def httpPost(
    path: String,
    postBody: String,
    accept: String = null,
    suppress: Boolean = false,
    contentType: String = Message.ContentTypeJson,
    headers: Map[String, String] = Map(),
    andExpect: Status = null,
    withLocation: String = null,
    withBody: String = null,
    withJsonBody: String = null,
    withJsonBodyNormalizer: JsonNode => JsonNode = null,
    withErrors: Seq[String] = null,
    routeHint: RouteHint = RouteHint.None,
    secure: Option[Boolean] = None
  ): Response = {

    val request = createApiRequest(path, Method.Post)
    request.setContentString(postBody)
    request.headerMap.set(Fields.ContentLength, request.content.length.toString)
    request.headerMap.set(Fields.ContentType, contentType)

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
      routeHint,
      secure = secure.getOrElse(defaultHttpSecure)
    )
  }

  /**
   * Performs a POST request to the embedded server serializing the normalized
   * response#contentString into an instance of type [[ResponseType]].
   *
   * @note Java users: see the more Java-friendly [[httpRequest(request: Request)]].
   * @see [[com.twitter.finatra.jackson.ScalaObjectMapper]]#parse[T: Manifest](string: String)
   * @param path - URI of the request
   * @param postBody - body of the POST request
   * @param suppress - suppress http client logging
   * @param headers - additional headers that should be passed with the request
   * @param andExpect - expected [[com.twitter.finagle.http.Status]] value
   * @param withLocation - expected response Location header value
   * @param withBody - expected body as a String
   * @param withJsonBody - expected body as JSON
   * @param withJsonBodyNormalizer - normalizer to use in conjunction with withJsonBody.
   * @param normalizeJsonParsedReturnValue - if the normalizer SHOULD be applied on the parsing of the
   *                                       response#contentString into type [[ResponseType]], default = false.
   * @param withErrors - expected errors
   * @param routeHint - optionally force the request to the main or admin interface of the embedded server, RouteHint.None by default
   * @param secure - use the https port to address the embedded server, default = None
   * @tparam ResponseType - parse the response#contentString into type [[ResponseType]]
   *
   * @return instance of type [[ResponseType]] serialized from the the response#contentString.
   */
  def httpPostJson[ResponseType: Manifest](
    path: String,
    postBody: String,
    suppress: Boolean = false,
    headers: Map[String, String] = Map(),
    andExpect: Status = Status.Ok,
    withLocation: String = null,
    withBody: String = null,
    withJsonBody: String = null,
    withJsonBodyNormalizer: JsonNode => JsonNode = null,
    normalizeJsonParsedReturnValue: Boolean = false,
    withErrors: Seq[String] = null,
    routeHint: RouteHint = RouteHint.None,
    secure: Option[Boolean] = None
  ): ResponseType = {

    assert(
      manifest[ResponseType] != manifest[Nothing],
      "httpPostJson requires a type-param to parse the JSON response into, e.g. http<Method>Json[MyCaseClass] or http<Method>Json[JsonNode]"
    )
    val response = httpPost(
      path,
      postBody,
      MediaType.JsonUtf8,
      suppress,
      Message.ContentTypeJson,
      headers,
      andExpect,
      withLocation,
      withBody,
      withJsonBody,
      withJsonBodyNormalizer,
      withErrors,
      routeHint,
      secure
    )

    jsonParseWithNormalizer(
      response,
      normalizeJsonParsedReturnValue,
      mapper,
      withJsonBodyNormalizer
    )
  }

  /**
   * Performs a PUT request to the embedded server.
   *
   * @note Java users: see the more Java-friendly [[httpRequest(request: Request)]].
   *
   * @param path - URI of the request
   * @param putBody - the body of the PUT request
   * @param accept - add request Accept header with the given [[com.twitter.finagle.http.MediaType]]
   * @param suppress - suppress http client logging
   * @param contentType - request Content-Type header value, application/json by default
   * @param headers - additional headers that should be passed with the request
   * @param andExpect - expected [[com.twitter.finagle.http.Status]] value
   * @param withLocation - expected response Location header value
   * @param withBody - expected body as a String
   * @param withJsonBody - expected body as JSON
   * @param withJsonBodyNormalizer - normalizer to use in conjunction with withJsonBody.
   * @param withErrors - expected errors
   * @param routeHint - optionally force the request to the main or admin interface of the embedded server, RouteHint.None by default
   * @param secure - use the https port to address the embedded server, default = None
   * @return a [[com.twitter.finagle.http.Response]] on success otherwise an exception
   *         if any of the assertions defined by andExpect or withXXXX fail
   */
  def httpPut(
    path: String,
    putBody: String,
    accept: String = null,
    suppress: Boolean = false,
    contentType: String = Message.ContentTypeJson,
    headers: Map[String, String] = Map(),
    andExpect: Status = null,
    withLocation: String = null,
    withBody: String = null,
    withJsonBody: String = null,
    withJsonBodyNormalizer: JsonNode => JsonNode = null,
    withErrors: Seq[String] = null,
    routeHint: RouteHint = RouteHint.None,
    secure: Option[Boolean] = None
  ): Response = {

    val request = createApiRequest(path, Method.Put)
    request.setContentString(putBody)
    request.headerMap.set(Fields.ContentLength, request.content.length.toString)
    request.headerMap.set(Fields.ContentType, contentType)

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
      routeHint,
      secure = secure.getOrElse(defaultHttpSecure)
    )
  }

  /**
   * Performs a PUT request to the embedded server serializing the normalized
   * response#contentString into an instance of type [[ResponseType]].
   *
   * @note Java users: see the more Java-friendly [[httpRequest(request: Request)]].
   * @see [[com.twitter.finatra.jackson.ScalaObjectMapper]]#parse[T: Manifest](string: String)
   * @param path - URI of the request
   * @param putBody - the body of the PUT request
   * @param suppress - suppress http client logging
   * @param headers - additional headers that should be passed with the request
   * @param andExpect - expected [[com.twitter.finagle.http.Status]] value
   * @param withLocation - expected response Location header value
   * @param withBody - expected body as a String
   * @param withJsonBody - expected body as JSON
   * @param withJsonBodyNormalizer - normalizer to use in conjunction with withJsonBody.
   * @param normalizeJsonParsedReturnValue - if the normalizer SHOULD be applied on the parsing of the
   *                                       response#contentString into type [[ResponseType]], default = false.
   * @param withErrors - expected errors
   * @param routeHint - optionally force the request to the main or admin interface of the embedded server, RouteHint.None by default
   * @param secure - use the https port to address the embedded server, default = None
   * @tparam ResponseType - parse the response#contentString into type [[ResponseType]]
   *
   * @return instance of type [[ResponseType]] serialized from the the response#contentString.
   */
  def httpPutJson[ResponseType: Manifest](
    path: String,
    putBody: String,
    suppress: Boolean = false,
    headers: Map[String, String] = Map(),
    andExpect: Status = Status.Ok,
    withLocation: String = null,
    withBody: String = null,
    withJsonBody: String = null,
    withJsonBodyNormalizer: JsonNode => JsonNode = null,
    normalizeJsonParsedReturnValue: Boolean = false,
    withErrors: Seq[String] = null,
    routeHint: RouteHint = RouteHint.None,
    secure: Option[Boolean] = None
  ): ResponseType = {

    assert(
      manifest[ResponseType] != manifest[Nothing],
      "httpPutJson requires a type-param to parse the JSON response into, e.g. httpPutJson[MyCaseClass] or httpPutJson[JsonNode]"
    )
    val response = httpPut(
      path,
      putBody,
      MediaType.JsonUtf8,
      suppress,
      Message.ContentTypeJson,
      headers,
      andExpect,
      withLocation,
      withBody,
      withJsonBody,
      withJsonBodyNormalizer,
      withErrors,
      routeHint,
      secure
    )

    jsonParseWithNormalizer(
      response,
      normalizeJsonParsedReturnValue,
      mapper,
      withJsonBodyNormalizer
    )
  }

  /**
   * Performs a DELETE request to the embedded server.
   *
   * @note Java users: see the more Java-friendly [[httpRequest(request: Request)]].
   *
   * @param path - URI of the request
   * @param deleteBody - the body of the DELETE request
   * @param accept - add request Accept header with the given [[com.twitter.finagle.http.MediaType]]
   * @param suppress - suppress http client logging
   * @param contentType - request Content-Type header value, application/json by default
   * @param headers - additional headers that should be passed with the request
   * @param andExpect - expected [[com.twitter.finagle.http.Status]] value
   * @param withLocation - expected response Location header value
   * @param withBody - expected body as a String
   * @param withJsonBody - expected body as JSON
   * @param withJsonBodyNormalizer - normalizer to use in conjunction with withJsonBody.
   * @param withErrors - expected errors
   * @param routeHint - optionally force the request to the main or admin interface of the embedded server, RouteHint.None by default
   * @param secure - use the https port to address the embedded server, default = None
   * @return a [[com.twitter.finagle.http.Response]] on success otherwise an exception
   *         if any of the assertions defined by andExpect or withXXXX fail
   */
  def httpDelete(
    path: String,
    deleteBody: String = null,
    accept: String = null,
    suppress: Boolean = false,
    contentType: String = Message.ContentTypeJson,
    headers: Map[String, String] = Map(),
    andExpect: Status = null,
    withLocation: String = null,
    withBody: String = null,
    withJsonBody: String = null,
    withJsonBodyNormalizer: JsonNode => JsonNode = null,
    withErrors: Seq[String] = null,
    routeHint: RouteHint = RouteHint.None,
    secure: Option[Boolean] = None
  ): Response = {

    val request = createApiRequest(path, Method.Delete)
    if (deleteBody != null) {
      request.setContentString(deleteBody)
      request.headerMap.set(Fields.ContentLength, request.content.length.toString)
      request.headerMap.set(Fields.ContentType, contentType)
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
      routeHint,
      secure = secure.getOrElse(defaultHttpSecure)
    )
  }

  /**
   * Performs a DELETE request to the embedded server serializing the normalized
   * response#contentString into an instance of type [[ResponseType]].
   *
   * @note Java users: see the more Java-friendly [[httpRequest(request: Request)]].
   * @see [[com.twitter.finatra.jackson.ScalaObjectMapper]]#parse[T: Manifest](string: String)
   * @param path - URI of the request
   * @param deleteBody - the body of the DELETE request
   * @param suppress - suppress http client logging
   * @param headers - additional headers that should be passed with the request
   * @param andExpect - expected [[com.twitter.finagle.http.Status]] value
   * @param withLocation - expected response Location header value
   * @param withBody - expected body as a String
   * @param withJsonBody - expected body as JSON
   * @param withJsonBodyNormalizer - normalizer to use in conjunction with withJsonBody.
   * @param normalizeJsonParsedReturnValue - if the normalizer SHOULD be applied on the parsing of the
   *                                       response#contentString into type [[ResponseType]], default = false.
   * @param withErrors - expected errors
   * @param routeHint - optionally force the request to the main or admin interface of the embedded server, RouteHint.None by default
   * @param secure - use the https port to address the embedded server, default = None
   * @tparam ResponseType - parse the response#contentString into type [[ResponseType]]
   *
   * @return instance of type [[ResponseType]] serialized from the the response#contentString.
   */
  def httpDeleteJson[ResponseType: Manifest](
    path: String,
    deleteBody: String,
    suppress: Boolean = false,
    headers: Map[String, String] = Map(),
    andExpect: Status = Status.Ok,
    withLocation: String = null,
    withBody: String = null,
    withJsonBody: String = null,
    withJsonBodyNormalizer: JsonNode => JsonNode = null,
    normalizeJsonParsedReturnValue: Boolean = false,
    withErrors: Seq[String] = null,
    routeHint: RouteHint = RouteHint.None,
    secure: Option[Boolean] = None
  ): ResponseType = {

    assert(
      manifest[ResponseType] != manifest[Nothing],
      "httpDeleteJson requires a type-param to parse the JSON response into, e.g. http<Method>Json[MyCaseClass] or http<Method>Json[JsonNode]"
    )
    val response = httpDelete(
      path,
      deleteBody,
      MediaType.JsonUtf8,
      suppress,
      Message.ContentTypeJson,
      headers,
      andExpect,
      withLocation,
      withBody,
      withJsonBody,
      withJsonBodyNormalizer,
      withErrors,
      routeHint,
      secure
    )

    jsonParseWithNormalizer(
      response,
      normalizeJsonParsedReturnValue,
      mapper,
      withJsonBodyNormalizer
    )
  }

  /**
   * Performs a OPTIONS request to the embedded server.
   *
   * @note Java users: see the more Java-friendly [[httpRequest(request: Request)]].
   *
   * @param path - URI of the request
   * @param accept - add request Accept header with the given [[com.twitter.finagle.http.MediaType]]
   * @param headers - additional headers that should be passed with the request
   * @param suppress - suppress http client logging
   * @param andExpect - expected [[com.twitter.finagle.http.Status]] value
   * @param withLocation - expected response Location header value
   * @param withBody - expected body as a String
   * @param withJsonBody - expected body as JSON
   * @param withJsonBodyNormalizer - normalizer to use in conjunction with withJsonBody
   * @param withErrors - expected errors
   * @param routeHint - optionally force the request to the main or admin interface of the embedded server, RouteHint.None by default
   * @param secure - use the https port to address the embedded server, default = None
   * @return a [[com.twitter.finagle.http.Response]] on success otherwise an exception
   *         if any of the assertions defined by andExpect or withXXXX fail
   */
  def httpOptions(
    path: String,
    accept: String = null,
    headers: Map[String, String] = Map(),
    suppress: Boolean = false,
    andExpect: Status = Status.Ok,
    withLocation: String = null,
    withBody: String = null,
    withJsonBody: String = null,
    withJsonBodyNormalizer: JsonNode => JsonNode = null,
    withErrors: Seq[String] = null,
    routeHint: RouteHint = RouteHint.None,
    secure: Option[Boolean] = None
  ): Response = {

    val request = createApiRequest(path, Method.Options)
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
      routeHint,
      secure = secure.getOrElse(defaultHttpSecure)
    )
  }

  /**
   * Performs a PATCH request to the embedded server.
   *
   * @note Java users: see the more Java-friendly [[httpRequest(request: Request)]].
   *
   * @param path - URI of the request
   * @param patchBody - the body of the PATCH request
   * @param accept - add request Accept header with the given [[com.twitter.finagle.http.MediaType]]
   * @param suppress - suppress http client logging
   * @param contentType - request Content-Type header value, application/json by default
   * @param headers - additional headers that should be passed with the request
   * @param andExpect - expected [[com.twitter.finagle.http.Status]] value
   * @param withLocation - expected response Location header value
   * @param withBody - expected body as a String
   * @param withJsonBody - expected body as JSON
   * @param withJsonBodyNormalizer - normalizer to use in conjunction with withJsonBody
   * @param withErrors - expected errors
   * @param routeHint - optionally force the request to the main or admin interface of the embedded server, RouteHint.None by default
   * @param secure - use the https port to address the embedded server, default = None
   * @return a [[com.twitter.finagle.http.Response]] on success otherwise an exception
   *         if any of the assertions defined by andExpect or withXXXX fail
   */
  def httpPatch(
    path: String,
    patchBody: String,
    accept: String = null,
    suppress: Boolean = false,
    contentType: String = Message.ContentTypeJson,
    headers: Map[String, String] = Map(),
    andExpect: Status = Status.Ok,
    withLocation: String = null,
    withBody: String = null,
    withJsonBody: String = null,
    withJsonBodyNormalizer: JsonNode => JsonNode = null,
    withErrors: Seq[String] = null,
    routeHint: RouteHint = RouteHint.None,
    secure: Option[Boolean] = None
  ): Response = {

    val request = createApiRequest(path, Method.Patch)
    request.setContentString(patchBody)
    request.headerMap.set(Fields.ContentLength, request.content.length.toString)
    request.headerMap.set(Fields.ContentType, contentType)

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
      routeHint,
      secure = secure.getOrElse(defaultHttpSecure)
    )
  }

  /**
   * Performs a PATCH request to the embedded server serializing the normalized
   * response#contentString into an instance of type [[ResponseType]].
   *
   * @note Java users: see the more Java-friendly [[httpRequest(request: Request)]].
   * @see [[com.twitter.finatra.jackson.ScalaObjectMapper]]#parse[T: Manifest](string: String)
   * @param path - URI of the request
   * @param patchBody - the body of the PATCH request
   * @param suppress - suppress http client logging
   * @param headers - additional headers that should be passed with the request
   * @param andExpect - expected [[com.twitter.finagle.http.Status]] value
   * @param withLocation - expected response Location header value
   * @param withBody - expected body as a String
   * @param withJsonBody - expected body as JSON
   * @param withJsonBodyNormalizer - normalizer to use in conjunction with withJsonBody.
   * @param normalizeJsonParsedReturnValue - if the normalizer SHOULD be applied on the parsing of the
   *                                       response#contentString into type [[ResponseType]], default = false
   * @param withErrors - expected errors
   * @param routeHint - optionally force the request to the main or admin interface of the embedded server, RouteHint.None by default
   * @param secure - use the https port to address the embedded server, default = None
   * @tparam ResponseType - parse the response#contentString into type [[ResponseType]]
   *
   * @return instance of type [[ResponseType]] serialized from the the response#contentString.
   */
  def httpPatchJson[ResponseType: Manifest](
    path: String,
    patchBody: String,
    suppress: Boolean = false,
    headers: Map[String, String] = Map(),
    andExpect: Status = Status.Ok,
    withLocation: String = null,
    withBody: String = null,
    withJsonBody: String = null,
    withJsonBodyNormalizer: JsonNode => JsonNode = null,
    normalizeJsonParsedReturnValue: Boolean = false,
    withErrors: Seq[String] = null,
    routeHint: RouteHint = RouteHint.None,
    secure: Option[Boolean] = None
  ): ResponseType = {

    assert(
      manifest[ResponseType] != manifest[Nothing],
      "httpPatchJson requires a type-param to parse the JSON response into, e.g. http<Method>Json[MyCaseClass] or http<Method>Json[JsonNode]"
    )
    val response = httpPatch(
      path,
      patchBody,
      MediaType.JsonUtf8,
      suppress,
      Message.ContentTypeJson,
      headers,
      andExpect,
      withLocation,
      withBody,
      withJsonBody,
      withJsonBodyNormalizer,
      withErrors,
      routeHint,
      secure
    )

    jsonParseWithNormalizer(
      response,
      normalizeJsonParsedReturnValue,
      mapper,
      withJsonBodyNormalizer
    )
  }

  /**
   * Performs a HEAD request to the embedded server.
   *
   * @note Java users: see the more Java-friendly [[httpRequest(request: Request)]].
   *
   * @param path - URI of the request
   * @param accept - add request Accept header with the given [[com.twitter.finagle.http.MediaType]]
   * @param headers - additional headers that should be passed with the request
   * @param suppress - suppress http client logging
   * @param andExpect - expected [[com.twitter.finagle.http.Status]] value
   * @param withLocation - expected response Location header value
   * @param withBody - expected body as a String
   * @param withJsonBody - expected body as JSON
   * @param withJsonBodyNormalizer - normalizer to use in conjunction with withJsonBody
   * @param withErrors - expected errors
   * @param routeHint - optionally force the request to the main or admin interface of the embedded server, RouteHint.None by default
   * @param secure - use the https port to address the embedded server, default = None
   * @return a [[com.twitter.finagle.http.Response]] on success otherwise an exception
   *         if any of the assertions defined by andExpect or withXXXX fail
   */
  def httpHead(
    path: String,
    accept: String = null,
    headers: Map[String, String] = Map(),
    suppress: Boolean = false,
    andExpect: Status = Status.Ok,
    withLocation: String = null,
    withBody: String = null,
    withJsonBody: String = null,
    withJsonBodyNormalizer: JsonNode => JsonNode = null,
    withErrors: Seq[String] = null,
    routeHint: RouteHint = RouteHint.None,
    secure: Option[Boolean] = None
  ): Response = {

    val request = createApiRequest(path, Method.Head)
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
      routeHint,
      secure = secure.getOrElse(defaultHttpSecure)
    )
  }

  /**
   * Performs a form POST request to the embedded server.
   *
   * @note Java users: see the more Java-friendly [[httpRequest(request: Request)]].
   *
   * @param path - URI of the request
   * @param params - a Map[String,String] of form params to send in the request
   * @param multipart - if this form post is a multi-part request, false by default
   * @param routeHint - optionally force the request to the main or admin interface of the embedded server, RouteHint.None by default
   * @param headers - additional headers that should be passed with the request
   * @param andExpect - expected [[com.twitter.finagle.http.Status]] value
   * @param withBody - expected body as a String
   * @param withJsonBody - expected body as JSON
   * @param secure - use the https port to address the embedded server, default = None
   * @return a [[com.twitter.finagle.http.Response]] on success otherwise an exception
   *         if any of the assertions defined by andExpect or withXXXX fail
   */
  def httpFormPost(
    path: String,
    params: Map[String, String],
    multipart: Boolean = false,
    routeHint: RouteHint = RouteHint.None,
    headers: Map[String, String] = Map.empty,
    andExpect: Status = Status.Ok,
    withBody: String = null,
    withJsonBody: String = null,
    secure: Option[Boolean] = None
  ): Response = {

    formPost(
      path = path,
      params = paramsToElements(params),
      multipart = multipart,
      routeHint = routeHint,
      headers = headers,
      andExpect = andExpect,
      withBody = withBody,
      withJsonBody = withJsonBody,
      secure = secure
    )
  }

  /**
   * Performs a multi-part form POST request to the embedded server.
   *
   * @note Java users: see the more Java-friendly [[httpRequest(request: Request)]].
   *
   * @param path - URI of the request
   * @param params - a Seq of [[com.twitter.finagle.http.FormElement]] to send in the request
   * @param multipart - if this form post is a multi-part request, true by default
   * @param routeHint - optionally force the request to the main or admin interface of the embedded server, RouteHint.None by default
   * @param headers - additional headers that should be passed with the request
   * @param andExpect - expected [[com.twitter.finagle.http.Status]] value
   * @param withBody - expected body as a String
   * @param withJsonBody - expected body as JSON
   * @param secure - use the https port to address the embedded server, default = None
   * @return a [[com.twitter.finagle.http.Response]] on success otherwise an exception
   *         if any of the assertions defined by andExpect or withXXXX fail
   */
  def httpMultipartFormPost(
    path: String,
    params: Seq[FormElement],
    multipart: Boolean = true,
    routeHint: RouteHint = RouteHint.None,
    headers: Map[String, String] = Map.empty,
    andExpect: Status = Status.Ok,
    withBody: String = null,
    withJsonBody: String = null,
    secure: Option[Boolean] = None
  ): Response = {

    formPost(
      path = path,
      params = params,
      multipart = multipart,
      routeHint = routeHint,
      headers = headers,
      andExpect = andExpect,
      withBody = withBody,
      withJsonBody = withJsonBody,
      secure = secure
    )
  }

  /**
   * For Java compatibility.
   *
   * @note Added to support tests from Java code which would need to manually set all arguments with default values
   * @param request a [[com.twitter.finagle.http.Request]] to send to the embedded HttpServer
   * @return the resultant [[com.twitter.finagle.http.Response]] returned from the embedded HttpServer
   */
  def httpRequest(request: Request): Response = {
    httpRequest(request, suppress = false)
  }

  /**
   * Sends the given [[com.twitter.finagle.http.Request]] to the embedded server.
   *
   * @note Java users: see the more Java-friendly [[httpRequest(request: Request)]].
   *
   * @param request - built [[com.twitter.finagle.http.Request]] to send to the embedded server
   * @param suppress - suppress http client logging
   * @param andExpect - expected [[com.twitter.finagle.http.Status]] value
   * @param withLocation - expected response Location header value
   * @param withBody - expected body as a String
   * @param withJsonBody - expected body as JSON
   * @param withJsonBodyNormalizer - normalizer to use in conjunction with withJsonBody
   * @param withErrors - expected errors
   * @param routeHint - optionally force the request to the main or admin interface of the embedded server, RouteHint.None by default
   * @param secure - use the https port to address the embedded server, default = None
   * @return a [[com.twitter.finagle.http.Response]] on success otherwise an exception
   *         if any of the assertions defined by andExpect or withXXXX fail
   */
  def httpRequest(
    request: Request,
    suppress: Boolean = false,
    andExpect: Status = null,
    withLocation: String = null,
    withBody: String = null,
    withJsonBody: String = null,
    withJsonBodyNormalizer: JsonNode => JsonNode = null,
    withErrors: Seq[String] = null,
    routeHint: RouteHint = RouteHint.None,
    secure: Option[Boolean] = None
  ): Response = {

    jsonAwareHttpExecute(
      request,
      request.headerMap.toMap,
      suppress,
      andExpect,
      withLocation,
      withBody,
      withJsonBody,
      withJsonBodyNormalizer,
      withErrors,
      routeHint,
      secure = secure.getOrElse(defaultHttpSecure)
    )
  }

  /**
   * Sends the given [[com.twitter.finagle.http.Request]] to the embedded server
   * serializing the normalized response#contentString into an instance of type [[ResponseType]]
   *
   * @see [[com.twitter.finatra.jackson.ScalaObjectMapper]]#parse[T: Manifest](string: String)
   * @param request - built [[com.twitter.finagle.http.Request]] to send to the embedded server
   * @param suppress - suppress http client logging
   * @param andExpect - expected [[com.twitter.finagle.http.Status]] value
   * @param withLocation - expected response Location header value
   * @param withBody - expected body as a String
   * @param withJsonBody - expected body as JSON
   * @param withJsonBodyNormalizer - normalizer to use in conjunction with withJsonBody
   * @param normalizeJsonParsedReturnValue - if the normalizer SHOULD be applied on the parsing of the
   *                                       response#contentString into type [[ResponseType]], default = false
   * @param withErrors - expected errors
   * @param routeHint - optionally force the request to the main or admin interface of the embedded server, RouteHint.None by default
   * @param secure - use the https port to address the embedded server, default = None
   *
   * @return instance of type [[ResponseType]] serialized from the response#contentString
   */
  def httpRequestJson[ResponseType: Manifest](
    request: Request,
    suppress: Boolean = false,
    andExpect: Status = null,
    withLocation: String = null,
    withBody: String = null,
    withJsonBody: String = null,
    withJsonBodyNormalizer: JsonNode => JsonNode = null,
    normalizeJsonParsedReturnValue: Boolean = false,
    withErrors: Seq[String] = null,
    routeHint: RouteHint = RouteHint.None,
    secure: Option[Boolean] = None
  ): ResponseType = {

    assert(
      manifest[ResponseType] != manifest[Nothing],
      "httpRequestJson requires a type-param to parse the JSON response into, e.g. http<Method>Json[MyCaseClass] or http<Method>Json[JsonNode]"
    )
    val response = httpRequest(
      request,
      suppress,
      andExpect,
      withLocation,
      withBody,
      withJsonBody,
      withJsonBodyNormalizer,
      withErrors,
      routeHint,
      secure
    )

    jsonParseWithNormalizer(
      response,
      normalizeJsonParsedReturnValue,
      mapper,
      withJsonBodyNormalizer
    )
  }

  // java-forwarder methods
  override final def bindClass[T](clazz: Class[T], instance: T): this.type =
    super.bindClass[T](clazz, instance)

  // java-forwarder methods
  override final def bindClass[T](clazz: Class[T], annotation: Annotation, instance: T): this.type =
    super.bindClass[T](clazz, annotation, instance)

  // java-forwarder methods
  override final def bindClass[T, Ann <: Annotation](
    clazz: Class[T],
    annotationClazz: Class[Ann],
    instance: T
  ): this.type =
    super.bindClass[T, Ann](clazz, annotationClazz, instance)

  // java-forwarder methods
  override final def bindClass[T, U <: T](clazz: Class[T], instanceClazz: Class[U]): this.type =
    super.bindClass[T, U](clazz, instanceClazz)

  // java-forwarder methods
  override final def bindClass[T, U <: T](
    clazz: Class[T],
    annotation: Annotation,
    instanceClazz: Class[U]
  ): this.type =
    super.bindClass[T, U](clazz, annotation, instanceClazz)

  // java-forwarder methods
  override final def bindClass[T, Ann <: Annotation, U <: T](
    clazz: Class[T],
    annotationClazz: Class[Ann],
    instanceClazz: Class[U]
  ): this.type =
    super.bindClass[T, Ann, U](clazz, annotationClazz, instanceClazz)

  /* Private */

  private def formPost(
    path: String,
    params: Seq[FormElement],
    multipart: Boolean,
    routeHint: RouteHint,
    headers: Map[String, String],
    andExpect: Status,
    withBody: String,
    withJsonBody: String,
    secure: Option[Boolean]
  ): Response = {
    val request = RequestBuilder()
      .url(EmbeddedHttpClient.normalizeURL(path))
      .addHeaders(headers)
      .add(params)
      .buildFormPost(multipart = multipart)

    jsonAwareHttpExecute(
      request,
      routeHint = routeHint,
      andExpect = andExpect,
      withBody = withBody,
      withJsonBody = withJsonBody,
      secure = secure.getOrElse(defaultHttpSecure)
    )
  }

  private def jsonAwareHttpExecute(
    request: Request,
    headers: Map[String, String] = Map(),
    suppress: Boolean = false,
    andExpect: Status = Status.Ok,
    withLocation: String = null,
    withBody: String = null,
    withJsonBody: String = null,
    withJsonBodyNormalizer: JsonNode => JsonNode = null,
    withErrors: Seq[String] = null, //TODO: Deprecate
    routeHint: RouteHint = RouteHint.None,
    secure: Boolean
  ): Response = {
    start() // ensure we have started the server
    val routeToAdminServer = (routeHint == RouteHint.AdminServer) ||
      (routeHint == RouteHint.None && matchesAdminRoute(request.method, request.path))
    if (routeToAdminServer) {
      httpAdminClient(
        request,
        headers,
        suppress,
        andExpect,
        withLocation,
        withBody
      )
    } else if (secure) {
      httpsClient(
        request,
        headers,
        suppress,
        andExpect,
        withLocation,
        withBody,
        withJsonBody,
        withJsonBodyNormalizer,
        withErrors
      )
    } else {
      httpClient(
        request,
        headers,
        suppress,
        andExpect,
        withLocation,
        withBody,
        withJsonBody,
        withJsonBodyNormalizer,
        withErrors
      )
    }
  }

  private def matchesAdminRoute(method: Method, path: String): Boolean = {
    path.startsWith(HttpRouter.FinatraAdminPrefix) ||
    adminHttpRouteMatchesPath(method -> path)
  }

  private[this] val adminHttpRouteMatchesPath: ((Method, String)) => Boolean =
    Memoize {
      case (method, path) =>
        adminHttpServerRoutes
          .exists(route => route.method == method && route.path == path)
    }

  private def paramsToElements(params: Map[String, String]): Seq[SimpleElement] = {
    params.map { case (k, v) => SimpleElement(k, v) }.toSeq
  }
}
