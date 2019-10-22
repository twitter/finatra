package com.twitter.finatra.http.routing

import com.twitter.finatra.http.internal.routing.Route

private[http] case class RoutesByType(external: Seq[Route], admin: Seq[Route])
