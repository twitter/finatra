package com.twitter.finatra.http.internal.exceptions

import com.twitter.finagle.http.{Request, Response}
import com.twitter.finatra.http.exceptions.HttpResponseException
import com.twitter.finatra.http.response.ResponseBuilder
import javax.inject.{Inject, Singleton}

@Singleton
private[http] class HttpResponseExceptionMapper @Inject() (response: ResponseBuilder)
    extends AbstractFrameworkExceptionMapper[HttpResponseException](response) {

  override protected def handle(
    request: Request,
    response: ResponseBuilder,
    exception: HttpResponseException
  ): Response = {
    response.create(exception.response)
  }
}
