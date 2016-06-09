package com.twitter.finatra.http.internal.exceptions.json

import com.fasterxml.jackson.core.JsonParseException
import com.twitter.finagle.http.{Request, Response}
import com.twitter.finatra.http.exceptions.ExceptionMapper
import com.twitter.finatra.http.response.ResponseBuilder
import com.twitter.finatra.json.internal.caseclass.jackson.JacksonUtils
import com.twitter.inject.Logging
import javax.inject.{Inject, Singleton}

@Singleton
class JsonParseExceptionMapper @Inject()(
  response: ResponseBuilder)
  extends ExceptionMapper[JsonParseException]
  with Logging {

  override def toResponse(request: Request, e: JsonParseException): Response = {
    warn(e)
    response
      .badRequest
      .jsonError(JacksonUtils.errorMessage(e))
  }
}
