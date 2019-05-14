package com.twitter.finatra.http

/**
 * Enumeration which determines which
 * server a given test request should be
 * sent to (ExternalServer or AdminServer).
 */
sealed trait RouteHint

object RouteHint {

  /** No hint is provided. Determination should be based on the route path */
  case object None extends RouteHint

  /** Request should be sent to the external server */
  case object ExternalServer extends RouteHint

  /** Request should be sent to the admin server */
  case object AdminServer extends RouteHint

}
