package com.twitter.finatra.http.internal.exceptions

import com.twitter.finagle.http.MediaType
import com.twitter.finagle.http.{Request, Response}
import com.twitter.finatra.http.exceptions.HttpException
import com.twitter.finatra.http.response.{ErrorsResponse, ResponseBuilder}
import javax.inject.{Inject, Singleton}

@Singleton
class HttpExceptionMapper @Inject()(response: ResponseBuilder)
    extends AbstractFrameworkExceptionMapper[HttpException](response) {

  override protected def handle(
    request: Request,
    response: ResponseBuilder,
    exception: HttpException
  ): Response = {
    val builder = response
      .status(exception.statusCode)
      .headers(exception.headers: _*)

    if (MediaType.typeEquals(exception.mediaType, MediaType.JsonUtf8))
      builder.json(ErrorsResponse(exception.errors))
    else
      builder.plain(exception.errors.mkString(", "))
  }
}
