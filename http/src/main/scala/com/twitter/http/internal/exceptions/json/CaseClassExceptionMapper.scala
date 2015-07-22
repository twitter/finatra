package com.twitter.finatra.http.internal.exceptions.json

import com.twitter.finagle.http.{Request, Response}
import com.twitter.finatra.http.exceptions.ExceptionMapper
import com.twitter.finatra.http.response.{ResponseBuilder, ErrorsResponse}
import com.twitter.finatra.json.internal.caseclass.exceptions.CaseClassMappingException
import javax.inject.{Inject, Singleton}

@Singleton
class CaseClassExceptionMapper @Inject()(
  response: ResponseBuilder)
  extends ExceptionMapper[CaseClassMappingException] {

  override def toResponse(request: Request, e: CaseClassMappingException): Response =
    response.badRequest.json(errorsResponse(e))

  private def errorsResponse(e: CaseClassMappingException): ErrorsResponse =
    ErrorsResponse(e.errors map {_.getMessage})
}
