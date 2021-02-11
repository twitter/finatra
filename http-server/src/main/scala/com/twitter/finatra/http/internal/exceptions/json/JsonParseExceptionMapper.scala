package com.twitter.finatra.http.internal.exceptions.json

import com.fasterxml.jackson.core.JsonParseException
import com.twitter.finagle.http.{Request, Response}
import com.twitter.finatra.http.exceptions.ExceptionMapper
import com.twitter.finatra.http.response.ResponseBuilder
import com.twitter.finatra.jackson.caseclass.exceptions._
import com.twitter.inject.Logging
import javax.inject.{Inject, Singleton}

@Singleton
private[http] class JsonParseExceptionMapper @Inject() (response: ResponseBuilder)
    extends ExceptionMapper[JsonParseException]
    with Logging {

  override def toResponse(request: Request, e: JsonParseException): Response = {
    warn(e)
    response.badRequest
      .jsonError(e.errorMessage)
  }
}
