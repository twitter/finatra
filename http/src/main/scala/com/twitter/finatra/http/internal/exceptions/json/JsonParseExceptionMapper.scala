package com.twitter.finatra.http.internal.exceptions.json

import com.fasterxml.jackson.core.JsonParseException
import com.twitter.finagle.httpx.{Request, Response}
import com.twitter.finatra.http.exceptions.ExceptionMapper
import com.twitter.finatra.http.response.{ResponseBuilder, ErrorsResponse}
import com.twitter.finatra.json.internal.caseclass.jackson.JacksonUtils
import javax.inject.{Inject, Singleton}

@Singleton
class JsonParseExceptionMapper @Inject()(
  response: ResponseBuilder)
  extends ExceptionMapper[JsonParseException] {

  override def toResponse(request: Request, e: JsonParseException): Response =
    response.badRequest.json(errorsResponse(e))

  private def errorsResponse(e: JsonParseException): ErrorsResponse =
    ErrorsResponse(JacksonUtils.errorMessage(e))
}
