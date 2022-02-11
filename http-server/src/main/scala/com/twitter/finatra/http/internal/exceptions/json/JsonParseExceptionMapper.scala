package com.twitter.finatra.http.internal.exceptions.json

import com.fasterxml.jackson.core.JsonParseException
import com.twitter.finagle.http.Request
import com.twitter.finagle.http.Response
import com.twitter.finatra.http.exceptions.ExceptionMapper
import com.twitter.finatra.http.response.ResponseBuilder
import com.twitter.util.logging.Logger
import com.twitter.util.jackson.caseclass.exceptions._
import javax.inject.Inject
import javax.inject.Singleton

private object JsonParseExceptionMapper {
  val logger: Logger = Logger(JsonParseExceptionMapper.getClass)
}

@Singleton
private[http] class JsonParseExceptionMapper @Inject() (response: ResponseBuilder)
    extends ExceptionMapper[JsonParseException] {
  import JsonParseExceptionMapper._

  override def toResponse(request: Request, e: JsonParseException): Response = {
    logger.warn(e.getMessage, e)
    response.badRequest
      .jsonError(e.errorMessage)
  }
}
