package com.twitter.finatra.http.internal.exceptions.json

import com.twitter.finagle.http.{Request, Response}
import com.twitter.finatra.http.exceptions.ExceptionMapper
import com.twitter.finatra.http.response.{ErrorsResponse, ResponseBuilder}
import com.twitter.util.jackson.caseclass.exceptions.CaseClassMappingException
import javax.inject.{Inject, Singleton}

@Singleton
private[http] class CaseClassExceptionMapper @Inject() (response: ResponseBuilder)
    extends ExceptionMapper[CaseClassMappingException] {

  override def toResponse(request: Request, e: CaseClassMappingException): Response =
    response.badRequest.json(errorsResponse(e))

  private[this] def errorsResponse(e: CaseClassMappingException): ErrorsResponse =
    ErrorsResponse(e.errors.map(_.getMessage))
}
