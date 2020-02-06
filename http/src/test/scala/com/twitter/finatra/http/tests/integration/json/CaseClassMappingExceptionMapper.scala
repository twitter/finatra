package com.twitter.finatra.http.tests.integration.json

import com.twitter.finagle.http.{Request, Response}
import com.twitter.finatra.http.exceptions.ExceptionMapper
import com.twitter.finatra.http.response.{ErrorsResponse, ResponseBuilder}
import com.twitter.finatra.jackson.caseclass.exceptions.CaseClassMappingException
import com.twitter.inject.Logging
import javax.inject.Inject

class CaseClassMappingExceptionMapper @Inject()(
  response: ResponseBuilder
) extends ExceptionMapper[CaseClassMappingException]
  with Logging {

  override def toResponse(request: Request, exception: CaseClassMappingException): Response = {
    response.badRequest(
      ErrorsResponse(
        toError(exception)))
  }

  private def toError(exception: CaseClassMappingException): Seq[String] = {
    for (error <- exception.errors) yield {
      val initialMessage = error.getMessage()
      if (initialMessage.contains("Cannot deserialize instance of")) {
        warn(initialMessage)
        s"${error.path.names.head}: Unable to parse"
      } else {
        initialMessage
      }
    }
  }
}
