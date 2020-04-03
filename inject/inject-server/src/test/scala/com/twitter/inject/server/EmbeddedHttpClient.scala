package com.twitter.inject.server

import com.twitter.conversions.DurationOps._
import com.twitter.finagle.{Http, Service}
import com.twitter.finagle.http.{Request, Response, Status}
import com.twitter.finagle.http.codec.HttpCodec
import com.twitter.finagle.stats.InMemoryStatsReceiver
import com.twitter.inject.server.PortUtils.loopbackAddressForPort
import com.twitter.util.{Await, Closable, Duration, Future, Stopwatch, Time}
import java.util.concurrent.TimeUnit.MILLISECONDS
import org.scalatest.matchers.should.Matchers

private[twitter] object EmbeddedHttpClient {
  def normalizeURL(path: String): String = {
    if (path.startsWith("http://"))
      path
    else
      "http://localhost:8080%s".format(path)
  }
}

/** Internal HTTP client used for making http requests to an [[EmbeddedTwitterServer]] */
private[twitter] class EmbeddedHttpClient private[twitter] (
  name: String,
  port: Int,
  tls: Boolean = false,
  sessionAcquisitionTimeout: Duration,
  streamResponses: Boolean,
  defaultHeaders: () => Map[String, String],
  disableLogging: Boolean)
    extends Matchers
    with Closable {

  /* Additional Constructors */

  private[twitter] def this(name: String, port: Int) =
    this(
      name = name,
      port = port,
      tls = false,
      sessionAcquisitionTimeout = 1.second,
      streamResponses = false,
      defaultHeaders = () => Map(),
      disableLogging = false
    )

  private[twitter] def this(name: String, port: Int, disableLogging: Boolean) =
    this(
      name = name,
      port = port,
      tls = false,
      sessionAcquisitionTimeout = 1.second,
      streamResponses = false,
      defaultHeaders = () => Map(),
      disableLogging = disableLogging
    )

  /* Mutable state */

  protected var _tls: Boolean = tls
  protected var _sessionAcquisitionTimeout: Duration = sessionAcquisitionTimeout
  protected var _streamResponses: Boolean = streamResponses
  protected var _defaultHeaders: () => Map[String, String] = defaultHeaders
  protected var _disableLogging: Boolean = disableLogging

  /* Fields */

  val label = s"$name:$port"
  private[this] var client =
    Http.client.withSessionQualifier.noFailFast.withSessionQualifier.noFailureAccrual.withSession
      .acquisitionTimeout(_sessionAcquisitionTimeout)
      .withStatsReceiver(new InMemoryStatsReceiver)
      .withStreaming(_streamResponses)
      .withLabel(label)
  if (tls) {
    client = configureTls(client)
  }

  private[twitter] val service: Service[Request, Response] =
    client.newService(PortUtils.loopbackAddressForPort(port))

  /* Public */

  def withTls(secure: Boolean): this.type = {
    this._tls = secure
    this
  }

  def withSessionAcquisitionTimeout(timeout: Duration): this.type = {
    this._sessionAcquisitionTimeout = timeout
    this
  }

  def withStreamResponses(streaming: Boolean): this.type = {
    this._streamResponses = streaming
    this
  }

  def withDefaultHeaders(headers: () => Map[String, String]): this.type = {
    this._defaultHeaders = headers
    this
  }

  def noLogging: this.type = {
    this._disableLogging = true
    this
  }

  def apply(request: Request): Response =
    apply(
      request,
      headers = Map(),
      suppress = false,
      andExpect = Status.Ok,
      withLocation = null,
      withBody = null
    )

  def apply(
    request: Request,
    headers: Map[String, String],
    suppress: Boolean,
    andExpect: Status,
    withLocation: String,
    withBody: String
  ): Response = {
    execute(request, headers, suppress, andExpect, withLocation, withBody)
  }

  /**
   * Close the resource with the given deadline. This deadline is advisory,
   * giving the callee some leeway, for example to drain clients or finish
   * up other tasks.
   */
  override def close(deadline: Time): Future[Unit] = {
    service.close(deadline)
  }

  /* Protected */

  protected final def execute(
    request: Request,
    headers: Map[String, String],
    suppress: Boolean,
    andExpect: Status,
    withLocation: String,
    withBody: String
  ): Response = {
    request.headerMap.set("Host", loopbackAddressForPort(port))

    /* Pre - Execute */

    // Don't overwrite request.headers potentially in given request */
    val defaults = _defaultHeaders().filter { case (key, _) => !request.headerMap.contains(key) }
    addOrRemoveHeaders(request, defaults)
    // headers added last so they can overwrite "defaults"
    addOrRemoveHeaders(request, headers)

    printRequest(request, suppress)

    /* Execute */
    val response = send(service, request)

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

  protected def prettyRequestBody(request: Request): String = {
    request.contentString
  }

  protected def printNonEmptyResponseBody(response: Response, suppress: Boolean): Unit = {
    info(response.contentString + "\n", suppress)
  }

  protected def disableLogging(suppress: Boolean): Boolean = suppress || this._disableLogging

  protected[twitter] def configureTls(client: Http.Client): Http.Client =
    client.withTlsWithoutValidation

  /* Private */

  // Deletes request headers with null values in map.
  private def addOrRemoveHeaders(request: Request, headers: Map[String, String]): Unit = {
    if (headers != null && headers.nonEmpty) {
      for ((key, value) <- headers) {
        if (value == null) {
          request.headerMap.remove(key)
        } else {
          request.headerMap.set(key, value)
        }
      }
    }
  }

  private def send(
    svc: Service[Request, Response],
    request: Request
  ): Response = {
    val f = svc(request)
    val elapsed = Stopwatch.start()
    try {
      Await.result(f)
    } catch {
      case t: Throwable =>
        println(
          s"ERROR in request: $request: ${t.getMessage} in ${elapsed().inUnit(MILLISECONDS)}ms"
        )
        throw t
    }
  }

  /* Request/Response Logging Utilities */

  private def printRequest(request: Request, suppress: Boolean): Unit = {
    val headers = request.headerMap.mkString("[Header]\t", "\n[Header]\t", "")

    val msg = "HTTP " + request.method + " " + request.uri + "\n" + headers
    if (request.contentString.isEmpty)
      infoBanner(msg, disableLogging(suppress))
    else
      infoBanner(msg + "\n" + prettyRequestBody(request), disableLogging(suppress))
  }

  private def printResponseMetadata(response: Response, suppress: Boolean): Unit = {
    info("-" * 75, disableLogging(suppress))
    info("[Status]\t" + response.status, disableLogging(suppress))
    info(response.headerMap.mkString("[Header]\t", "\n[Header]\t", ""), disableLogging(suppress))
  }

  private def printResponseBody(response: Response, suppress: Boolean): Unit = {
    if (response.isChunked) {
      //no-op
    } else if (response.contentString.isEmpty) {
      info("*EmptyBody*", disableLogging(suppress))
    } else {
      printNonEmptyResponseBody(response, disableLogging(suppress))
    }
  }

  private def receivedResponseStr(response: Response): String = {
    "\n\nReceived Response:\n" + HttpCodec.encodeResponseToString(response)
  }
}
