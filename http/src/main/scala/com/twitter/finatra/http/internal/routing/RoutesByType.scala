package com.twitter.finatra.http.internal.routing

case class RoutesByType(
  external: Seq[Route],
  admin: Seq[Route])
