package com.twitter.finatra.http.internal.request

import com.twitter.finagle.httpx.{ParamMap, Request, RequestProxy}

class RequestWithPathParams(
  wrapped: Request,
  incomingParams: Map[String, String])
  extends RequestProxy {

  override lazy val params: ParamMap = {
    new RouteParamMap(
      super.params,
      incomingParams)
  }

  def request: Request = wrapped
}
