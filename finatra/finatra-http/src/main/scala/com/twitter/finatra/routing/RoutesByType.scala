package com.twitter.finatra.routing

case class RoutesByType(
  external: Seq[Route],
  admin: Seq[Route])
