package com.twitter.finatra.http.tests.integration.doeverything.main.filters

import com.twitter.finagle.http.{Request, Response}
import com.twitter.finagle.{Service, SimpleFilter}
import com.twitter.finatra.http.response.ResponseBuilder
import com.twitter.util.Future
import javax.inject.Inject

class ForbiddenFilter @Inject()(responseBuilder: ResponseBuilder) extends SimpleFilter[Request, Response] {
  def apply(request: Request, service: Service[Request, Response]): Future[Response] = {
    Future.value(responseBuilder.forbidden)
  }
}
