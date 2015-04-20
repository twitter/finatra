package com.twitter.finatra.internal.exceptions.json

import com.twitter.finagle.http.{Request, Response}
import com.twitter.finatra.exceptions.ExceptionMapper
import com.twitter.finatra.json.internal.caseclass.exceptions.JsonObjectParseException
import com.twitter.finatra.response.{ResponseBuilder, ErrorsResponse}
import javax.inject.{Inject, Singleton}

@Singleton
class JsonObjectParseExceptionMapper @Inject()(
  response: ResponseBuilder)
  extends ExceptionMapper[JsonObjectParseException] {

  override def toResponse(request: Request, e: JsonObjectParseException): Response =
    response.badRequest.json(errorsResponse(e))
  
  private def errorsResponse(e: JsonObjectParseException): ErrorsResponse =
    ErrorsResponse((e.fieldErrors ++ e.methodValidationErrors) map {_.getMessage})
}
