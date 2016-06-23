package com.twitter.finatra.http.internal.routing

private[http] case class RoutesByType(
  external: Seq[Route],
  admin: Seq[Route])
