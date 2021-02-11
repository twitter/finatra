package com.twitter.finatra.http

import com.fasterxml.jackson.databind.JsonNode
import com.twitter.conversions.DurationOps._
import com.twitter.finagle.http.{Request, Response, Status}
import com.twitter.finatra.jackson.ScalaObjectMapper
import com.twitter.finatra.json.JsonDiff
import com.twitter.inject.server.PortUtils.loopbackAddressForPort
import com.twitter.inject.server.{EmbeddedHttpClient, _}
import com.twitter.util.{Duration, Try}

private[finatra] object JsonAwareEmbeddedHttpClient {
  def jsonParseWithNormalizer[T: Manifest](
    response: Response,
    normalizeParsedJsonNode: Boolean = true,
    mapper: ScalaObjectMapper = ScalaObjectMapper(),
    normalizer: JsonNode => JsonNode = identity[JsonNode]
  ): T = {
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
        println(
          s"Json parsing error $e trying to parse response $response with body " + response.contentString
        )
        throw e
    }
  }
}

/** Internal HTTP client used for making http requests to an [[EmbeddedHttpServer]] */
private[finatra] class JsonAwareEmbeddedHttpClient private[finatra] (
  name: String,
  port: Int,
  tls: Boolean = false,
  sessionAcquisitionTimeout: Duration,
  streamResponses: Boolean,
  defaultHeaders: () => Map[String, String],
  mapper: ScalaObjectMapper,
  disableLogging: Boolean)
    extends EmbeddedHttpClient(
      name,
      port,
      tls,
      sessionAcquisitionTimeout,
      streamResponses,
      defaultHeaders,
      disableLogging
    ) {

  /* Additional Constructors */

  private[finatra] def this(name: String, port: Int) =
    this(
      name = name,
      port = port,
      tls = false,
      sessionAcquisitionTimeout = 1.second,
      streamResponses = false,
      defaultHeaders = () => Map(),
      mapper = ScalaObjectMapper(),
      disableLogging = false
    )

  private[finatra] def this(name: String, port: Int, disableLogging: Boolean) =
    this(
      name = name,
      port = port,
      tls = false,
      sessionAcquisitionTimeout = 1.second,
      streamResponses = false,
      defaultHeaders = () => Map(),
      mapper = ScalaObjectMapper(),
      disableLogging = disableLogging
    )

  /* Mutable state */
  private[this] var _mapper = mapper

  def withMapper(mapper: ScalaObjectMapper): this.type = {
    this._mapper = mapper
    this
  }

  override def apply(request: Request): Response =
    apply(
      request,
      headers = Map(),
      suppress = false,
      andExpect = Status.Ok,
      withLocation = null,
      withBody = null,
      withJsonBody = null,
      withJsonBodyNormalizer = null,
      withErrors = null
    )

  def apply(
    request: Request,
    headers: Map[String, String],
    suppress: Boolean,
    andExpect: Status,
    withLocation: String,
    withBody: String,
    withJsonBody: String,
    withJsonBodyNormalizer: JsonNode => JsonNode,
    withErrors: Seq[String]
  ): Response = {
    execute(
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

  private final def execute(
    request: Request,
    headers: Map[String, String],
    suppress: Boolean,
    andExpect: Status,
    withLocation: String,
    withBody: String,
    withJsonBody: String,
    withJsonBodyNormalizer: JsonNode => JsonNode,
    withErrors: Seq[String]
  ): Response = {
    request.headerMap.set("Host", loopbackAddressForPort(port))

    val response = super.execute(request, headers, suppress, andExpect, withLocation, withBody)

    if (withJsonBody != null) {
      // expecting JSON, it is fine to expect empty JSON.
      if (withJsonBody.nonEmpty) {
        JsonDiff.jsonDiff(
          response.contentString,
          withJsonBody,
          withJsonBodyNormalizer,
          verbose = false
        )
      } else {
        response.contentString should equal("")
      }
    }

    if (nonEmpty(withErrors)) {
      JsonDiff.jsonDiff(response.contentString, Map("errors" -> withErrors), withJsonBodyNormalizer)
    }

    response
  }

  /* Overrides */

  override protected def prettyRequestBody(request: Request): String = {
    val printableBody = request.contentString
      .replaceAll("[\\p{Cntrl}&&[^\n\t\r]]", "?") //replace non-printable characters

    Try {
      _mapper.writePrettyString(printableBody)
    } getOrElse {
      printableBody
    }
  }

  override protected def printNonEmptyResponseBody(response: Response, suppress: Boolean): Unit = {
    try {
      info(_mapper.writePrettyString(response.getContentString()), disableLogging(suppress))
    } catch {
      case _: Exception =>
        info(response.contentString, disableLogging(suppress))
    }
    info("", disableLogging(suppress))
  }
}
