package com.twitter.finatra.http.request

import com.twitter.finagle.http.{Response, Request}
import com.twitter.finatra.http.internal.request.ForwardedRequest
import com.twitter.finatra.http.routing.HttpRouter
import com.twitter.inject.Logging
import com.twitter.util.Future
import javax.inject.{Singleton, Inject}

@Singleton
class HttpForward @Inject()(router: HttpRouter) extends Logging {

  /**
   * Forwards the given [[com.twitter.finagle.http.Request]] to the given path.
   *
   * @note if the [[com.twitter.finatra.http.filters.StatsFilter]] is included
   * in the server filter chain, any per-route stats will be reported as the
   * "forwarded-to" route as the stats are written on the response path.
   * @param request the [[com.twitter.finagle.http.Request]] to forward.
   * @param path the path of the route to which to forward
   * @return the [[com.twitter.finagle.http.Response]] from the
   *         "forwarded-to" route.
   */
  def apply(request: Request, path: String): Future[Response] = {
    val forwardedRequest = new ForwardedRequest(request, path)
    if (path.startsWith("/admin")) {
      router.adminRoutingService.route(forwardedRequest, bypassFilters = true)
    } else {
      router.externalRoutingService.route(forwardedRequest, bypassFilters = true)
    }
  }
}
