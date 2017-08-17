package com.twitter.finatra.http.jsonpatch

import com.twitter.finagle.http.{Request, Response}
import com.twitter.finatra.http.exceptions.ExceptionMapper
import com.twitter.finatra.http.response.{ErrorsResponse, ResponseBuilder}
import javax.inject.{Inject, Singleton}

@Singleton
class JsonPatchExceptionMapper @Inject()(response: ResponseBuilder)
    extends ExceptionMapper[JsonPatchException] {

  override def toResponse(request: Request, e: JsonPatchException): Response =
    response.badRequest.json(ErrorsResponse(e.getMessage))
}
