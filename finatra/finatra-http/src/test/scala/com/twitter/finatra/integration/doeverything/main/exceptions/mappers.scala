package com.twitter.finatra.integration.doeverything.main.exceptions

import com.twitter.finagle.http.{Request, Response}
import com.twitter.finatra.exceptions.ExceptionMapper
import com.twitter.finatra.response.ResponseBuilder
import javax.inject.Inject

class BarExceptionMapper @Inject()(response: ResponseBuilder)
  extends ExceptionMapper[BarException] {

  override def toResponse(request: Request, exception: BarException): Response = {
    response.unauthorized("bar").header("Bar-ID", exception.id)
  }
}

class FooExceptionMapper @Inject()(response: ResponseBuilder)
  extends ExceptionMapper[FooException] {

  override def toResponse(request: Request, exception: FooException): Response = {
    response.forbidden("foo").header("Foo-ID", exception.id)
  }
}
