package com.twitter.finatra.twitterserver.routing

import com.twitter.finagle.Service
import com.twitter.finagle.http.{Request, Response}

case class Services(
  adminRoutes: Seq[Route],
  externalRoutes : Seq[Route],
  adminService: Service[Request, Response],
  externalService: Service[Request, Response])
