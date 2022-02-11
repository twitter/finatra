package com.twitter.finatra.http.tests.integration.json

import com.twitter.finagle.http.Request
import com.twitter.finagle.http.Response
import com.twitter.finatra.http.exceptions.ExceptionMapper
import com.twitter.finatra.http.response.ErrorsResponse
import com.twitter.finatra.http.response.ResponseBuilder
import com.twitter.util.jackson.caseclass.exceptions.CaseClassMappingException
import com.twitter.util.logging.Logging
import javax.inject.Inject

class CaseClassMappingExceptionMapper @Inject() (
  response: ResponseBuilder)
    extends ExceptionMapper[CaseClassMappingException]
    with Logging {

  override def toResponse(request: Request, exception: CaseClassMappingException): Response = {
    response.badRequest(ErrorsResponse(toError(exception)))
  }

  private def toError(exception: CaseClassMappingException): Seq[String] = {
    for (error <- exception.errors) yield {
      val initialMessage = error.getMessage()
      if (initialMessage.contains("Cannot deserialize value of") ||
        initialMessage.contains("Cannot deserialize instance of")) {
        warn(initialMessage)
        s"${error.path.names.head}: Unable to parse"
      } else {
        initialMessage
      }
    }
  }
}
