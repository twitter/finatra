package com.twitter.finatra.http.routing

import com.twitter.finagle.http.{Request, Response}
import com.twitter.finatra.http.exceptions.MaxForwardsExceededException
import com.twitter.finatra.http.request.ForwardedRequest
import com.twitter.inject.Logging
import com.twitter.util.Future
import javax.inject.{Inject, Singleton}

object HttpForward {
  private[finatra] val DepthField: Request.Schema.Field[Option[Int]] =
    Request.Schema.newField[Option[Int]](None)
}

@Singleton
class HttpForward @Inject()(router: HttpRouter) extends Logging {
  import HttpForward._

  /**
   * Forwards the given [[com.twitter.finagle.http.Request]] to the given path.
   *
   * @note There is a maximum depth as specified by
   *       [[HttpRouter.withMaxRequestForwardingDepth(depth: Int)]].
   * @note if the [[com.twitter.finatra.http.filters.StatsFilter]] is included
   * in the server filter chain, any per-route stats will be reported as the
   * "forwarded-to" route as the stats are written on the response path.
   * @param request the [[com.twitter.finagle.http.Request]] to forward.
   * @param path the path of the route to which to forward
   *
   * @return the [[com.twitter.finagle.http.Response]] from the
   *         "forwarded-to" route.
   */
  def apply(request: Request, path: String): Future[Response] = {
    val maxDepth: Int = router.maxRequestForwardingDepth

    request.ctx(DepthField) match {
      case Some(depth) if depth >= maxDepth =>
        Future.exception(new MaxForwardsExceededException(maxDepth))
      case Some(depth) =>
        request.ctx.update(DepthField, Some(depth + 1))
        forward(request, path)
      case _ =>
        request.ctx.update(DepthField, Some(1))
        forward(request, path)
    }
  }

  private def forward(request: Request, path: String): Future[Response] = {
    val forwardedRequest = new ForwardedRequest(request, path)

    if (path.startsWith("/admin")) {
      router.adminRoutingService.route(forwardedRequest, bypassFilters = true)
    } else {
      router.externalRoutingService.route(forwardedRequest, bypassFilters = true)
    }
  }
}
