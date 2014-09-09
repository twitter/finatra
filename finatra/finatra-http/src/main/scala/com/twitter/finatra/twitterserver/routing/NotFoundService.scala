package com.twitter.finatra.twitterserver.routing

import com.twitter.finagle.Service
import com.twitter.finagle.http.{Request, Response, Status}
import com.twitter.finatra.response.SimpleResponse
import com.twitter.util.Future

object NotFoundService {
  def notFound(uri: String) = {
    SimpleResponse(
      Status.NotFound,
      uri + " route not found")
  }
}

class NotFoundService extends Service[Request, Response] {

  def apply(request: Request) = {
    Future.value(
      NotFoundService.notFound(request.uri))
  }
}

