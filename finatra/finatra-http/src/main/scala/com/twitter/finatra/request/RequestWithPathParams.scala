package com.twitter.finatra.request

import com.twitter.finagle.http.{ParamMap, RequestProxy, Request => FinagleRequest}

class RequestWithPathParams(
  override val request: FinagleRequest,
  incomingParams: Map[String, String])
  extends RequestProxy {

  override lazy val params: ParamMap = {
    new RouteParamMap(
      super.params,
      incomingParams)
  }
}
