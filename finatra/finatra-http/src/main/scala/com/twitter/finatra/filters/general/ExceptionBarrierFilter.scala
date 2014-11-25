package com.twitter.finatra.filters.general

import com.fasterxml.jackson.core.JsonProcessingException
import com.twitter.finagle.http.{Request, Response}
import com.twitter.finagle.stats.StatsReceiver
import com.twitter.finagle.{CancelledRequestException, Service, SimpleFilter}
import com.twitter.finatra.exceptions._
import com.twitter.finatra.json.FinatraObjectMapper
import com.twitter.finatra.json.internal.caseclass.exceptions.{JsonInjectException, JsonInjectionNotSupportedException, JsonObjectParseException}
import com.twitter.finatra.json.internal.caseclass.jackson.JacksonUtils
import com.twitter.finatra.response._
import com.twitter.finatra.utils.Logging
import com.twitter.util.{Future, Memoize, NonFatal}
import javax.inject.{Inject, Singleton}

/**
 * Filter which converts exceptions into HTTP responses.
 * Should be first filter in the chain.
 *
 * TODO Implement exception mappers
 */
@Singleton
class ExceptionBarrierFilter @Inject()(
  statsReceiver: StatsReceiver,
  mapper: FinatraObjectMapper,
  response: ResponseBuilder)
  extends SimpleFilter[Request, Response]
  with Logging {

  private val responseCodeStatsReceiver = statsReceiver.scope("server/response/status")

  /* Public */

  def apply(request: Request, service: Service[Request, Response]): Future[Response] = {
    service(request) handle {
      case e: HttpException =>
        e.createResponse(response)
      case e: HttpResponseException =>
        e.response
      case e: JsonProcessingException =>
        response.badRequest.json(
          errorsResponse(e))
      case e: JsonObjectParseException =>
        response.badRequest.json(
          errorsResponse(e))
      case e: JsonInjectException =>
        internalServerError(request, e, logStackTrace = false)
      case e: JsonInjectionNotSupportedException =>
        internalServerError(request, e)
      case e: CancelledRequestException =>
        response.clientClosed
      case ExternalServiceExceptionMatcher(e) =>
        response.serviceUnavailable.json(
          errorsResponse(request, e, "service unavailable"))
      case NonFatal(e) =>
        internalServerError(request, e)
      case e: NoSuchMethodException =>
        internalServerError(request, e)
    } onSuccess { response =>
      statusCodeCounter(response.status.getCode).incr()
    }
  }

  /* Private */

  private val statusCodeCounter = Memoize { statusCode: Int =>
    responseCodeStatsReceiver.counter(statusCode.toString)
  }

  private def internalServerError(request: Request, throwable: Throwable, logStackTrace: Boolean = true) = {
    response.internalServerError.json(
      errorsResponse(request, throwable, "internal server error", logStackTrace))
  }

  private def errorsResponse(request: Request, throwable: Throwable, errorStr: String, logStackTrace: Boolean = true): ErrorsResponse = {
    if (logStackTrace)
      error(errorStr, throwable)
    else
      error(errorStr + ": " + throwable.getMessage)

    if (request.path.startsWith("/admin"))
      ErrorsResponse(throwable.getMessage)
    else
      ErrorsResponse(errorStr)
  }

  private def errorsResponse(e: JsonProcessingException): ErrorsResponse = {
    ErrorsResponse(
      JacksonUtils.errorMessage(e))
  }

  private def errorsResponse(e: JsonObjectParseException): ErrorsResponse = {
    ErrorsResponse((e.fieldErrors ++ e.methodValidationErrors) map {_.getMessage})
  }
}
