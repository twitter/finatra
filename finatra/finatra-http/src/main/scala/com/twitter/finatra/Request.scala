package com.twitter.finatra

import com.twitter.finagle.http.{RequestProxy, Request => FinagleRequest}
import com.twitter.finatra.request.RouteParams

case class Request(
  request: FinagleRequest,
  routeParams: RouteParams = RouteParams.empty)
  extends RequestProxy