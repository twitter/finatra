package com.twitter.finatra.http.tests.integration.tweetexample.main.filters

import com.twitter.finagle.http.{Request, Response}
import com.twitter.finagle.{Service, SimpleFilter}
import com.twitter.finatra.http.response.ResponseBuilder
import com.twitter.util.Future
import javax.inject.Inject

class AuthFilter @Inject()(
  responseBuilder: ResponseBuilder)
  extends SimpleFilter[Request, Response] {

  def apply(request: Request, service: Service[Request, Response]): Future[Response] = {
    if (request.headerMap.contains("X-UserId")) {
      service(request)
    }
    else {
      responseBuilder.
        unauthorized("user id not found").toFuture
    }
  }
}
