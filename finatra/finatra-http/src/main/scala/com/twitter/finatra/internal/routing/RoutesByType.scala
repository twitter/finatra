package com.twitter.finatra.internal.routing

case class RoutesByType(
  external: Seq[Route],
  admin: Seq[Route])
