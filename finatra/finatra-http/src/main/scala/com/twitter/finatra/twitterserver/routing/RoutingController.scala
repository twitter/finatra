package com.twitter.finatra.twitterserver.routing

import com.twitter.finagle.Service
import com.twitter.finagle.http.{Request, Response}
import com.twitter.finatra.utils.Logging
import com.twitter.util.Future

//TODO: Optimize by partitioning routes per HTTP method
class RoutingController(
  routes: Seq[Route],
  notFoundService: Service[Request, Response] = new NotFoundService)
  extends Service[Request, Response]
  with Logging {

  private val routePathsStr = routes map {_.path} mkString ", "

  override def apply(request: Request): Future[Response] = {
    for (route <- routes) {
      val responseOpt = route.handle(request)
      if (responseOpt.isDefined) {
        return responseOpt.get
      }
    }

    debug(request + " not found in " + routePathsStr)
    notFoundService(request)
  }
}
