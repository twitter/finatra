package com.twitter.finatra.http.internal.routing

import com.twitter.finagle.Service
import com.twitter.finagle.http.{Request, Response}

private[http] case class Services(
  routesByType: RoutesByType,
  adminService: Service[Request, Response],
  externalService: Service[Request, Response])
