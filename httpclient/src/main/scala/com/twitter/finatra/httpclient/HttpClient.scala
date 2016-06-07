package com.twitter.finatra.httpclient

import com.twitter.finagle.Service
import com.twitter.finagle.http.{Request, Response, Status}
import com.twitter.finagle.service.RetryPolicy
import com.twitter.finatra.conversions.future._
import com.twitter.finatra.json.FinatraObjectMapper
import com.twitter.finatra.utils.RetryUtils
import com.twitter.inject.Logging
import com.twitter.util.{Future, Try}

/**
 * A simple HTTP client
 */
class HttpClient(
  hostname: String = "",
  httpService: Service[Request, Response],
  retryPolicy: Option[RetryPolicy[Try[Response]]] = None,
  defaultHeaders: Map[String, String] = Map(),
  mapper: FinatraObjectMapper)
  extends Logging {

  /* Public */

  def execute(request: Request): Future[Response] = {
    debug(request + " with headers: " + request.headerMap.mkString(", "))
    setHeaders(request)
    setHostname(request)

    retryPolicy match {
      case Some(policy) => RetryUtils.retryFuture(policy)(httpService(request))
      case _ => httpService(request)
    }
  }

  def executeJson[T: Manifest](
    request: Request,
    expectedStatus: Status = Status.Ok): Future[T] = {

    execute(request) flatMap { httpResponse =>
      if (httpResponse.status != expectedStatus) {
        Future.exception(new HttpClientException(
          httpResponse.status,
          httpResponse.contentString))
      }
      else {
        Future(
          FinatraObjectMapper.parseResponseBody[T](httpResponse, mapper.reader[T]))
            .transformException { e =>
              new HttpClientException(
                httpResponse.status,
                s"${e.getClass.getName} - ${e.getMessage}")
        }
      }
    }
  }

  @deprecated("Use execute(RequestBuilder.get(...))", "")
  def get(uri: String, headers: Seq[(String, String)] = Seq()): Future[Response] = {
    execute(RequestBuilder
      .get(uri)
      .headers(headers).request)
  }

  /* Private */

  private def setHostname(request: Request) = {
    if (hostname.nonEmpty) {
      request.headerMap.set("Host", hostname)
    }
  }

  private def setHeaders(request: Request): Unit = {
    if (defaultHeaders.nonEmpty) {
      for ((key, value) <- defaultHeaders) {
        request.headerMap.set(key, value)
      }
    }
  }
}
