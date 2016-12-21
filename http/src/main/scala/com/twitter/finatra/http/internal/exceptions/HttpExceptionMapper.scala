package com.twitter.finatra.http.internal.exceptions

import com.google.common.net.MediaType
import com.twitter.finagle.http.{Request, Response}
import com.twitter.finatra.http.exceptions.HttpException
import com.twitter.finatra.http.response.{ErrorsResponse, ResponseBuilder}
import javax.inject.{Inject, Singleton}

@Singleton
class HttpExceptionMapper @Inject()(
  response: ResponseBuilder)
  extends AbstractExceptionMapper[HttpException](response) {

  override protected def handle(
    request: Request,
    response: ResponseBuilder,
    exception: HttpException): Response = {
    val builder = response.status(exception.statusCode)
    if (exception.mediaType.is(MediaType.JSON_UTF_8))
      builder.json(ErrorsResponse(exception.errors))
    else
      builder.plain(exception.errors.mkString(", "))
  }
}