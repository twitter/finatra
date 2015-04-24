package com.twitter.finatra.internal.routing

import com.twitter.finagle.Service
import com.twitter.finagle.http.{Request, Response}

case class Services(
  routesByType: RoutesByType,
  adminService: Service[Request, Response],
  externalService: Service[Request, Response])
