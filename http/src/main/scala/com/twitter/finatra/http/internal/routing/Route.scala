package com.twitter.finatra.http.internal.routing

import com.twitter.finagle.http.{Method, Request, Response, RouteIndex}
import com.twitter.finagle.{Filter, Service}
import com.twitter.finatra.http.contexts.RouteInfo
import com.twitter.finatra.http.internal.request.RequestWithRouteParams
import com.twitter.finatra.http.internal.routing.Route._
import com.twitter.util.Future
import java.lang.annotation.Annotation
import scala.language.existentials
import scala.reflect.ClassTag

private[http] object Route {
  val OptionalTrailingSlashIdentifier = "/?"
}

//optimized
private[http] case class Route(
  name: String,
  method: Method,
  uri: String,
  clazz: Class[_],
  admin: Boolean,
  index: Option[RouteIndex],
  callback: Request => Future[Response],
  annotations: Seq[Annotation] = Seq(),
  requestClass: ClassTag[_],
  responseClass: ClassTag[_],
  routeFilter: Filter[Request, Response, Request, Response], // specific filter chain defined for this route
  filter: Filter[Request, Response, Request, Response] // global filter chain to apply to this route
) {

  val path: String = normalizeUriToPath(uri)

  private[this] val pattern = PathPattern(path)
  private[this] val routeInfo = RouteInfo(name, path)

  private[this] val callbackService: Service[Request, Response] =
    Service.mk[Request, Response](callback)
  private[this] val filteredRouteCallback: Request => Future[Response] =
    routeFilter.andThen(callbackService)
  private[this] val filteredCallback: Request => Future[Response] =
    filter.andThen(callbackService)

  /* Public */

  val captureNames: Seq[String] = pattern.captureNames

  val constantRoute: Boolean = captureNames.isEmpty

  val hasOptionalTrailingSlash: Boolean = uri.endsWith(OptionalTrailingSlashIdentifier)

  val summary: String = f"$method%-7s $uri"

  /** Prepends the incoming Filter to the contained Filter chain */
  def withFilter(filter: Filter[Request, Response, Request, Response]): Route = {
    this.copy(filter = filter.andThen(this.filter))
  }

  // Note: incomingPath is an optimization to avoid calling request.path for every potential route
  def handle(
    request: Request,
    incomingPath: String,
    bypassFilters: Boolean
  ): Option[Future[Response]] = {
    val path = toMatchPath(incomingPath)
    val routeParamsOpt = pattern.extract(path)

    if (routeParamsOpt.isEmpty) {
      None
    } else {
      handleMatch(createRequest(request, routeParamsOpt.get), bypassFilters)
    }
  }

  def handleMatch(request: Request, bypassFilters: Boolean): Some[Future[Response]] = {
    RouteInfo.set(request, routeInfo)
    if (bypassFilters)
      Some(filteredRouteCallback(request))
    else
      Some(filteredCallback(request))
  }

  /* Private */

  private[this] def createRequest(request: Request, routeParams: Map[String, String]) = {
    if (routeParams.isEmpty)
      request
    else
      new RequestWithRouteParams(request, routeParams)
  }

  /** routes are stored with the optional trailing slash, thus we add it to match if not present */
  private[this] def toMatchPath(incomingPath: String): String = {
    if (hasOptionalTrailingSlash && !incomingPath.endsWith("/"))
      incomingPath + "/"
    else
      incomingPath
  }

  /** normalize a URI to a route path */
  private[this] def normalizeUriToPath(uri: String): String = {
    if (uri.endsWith(OptionalTrailingSlashIdentifier)) {
      // store path with trailing slash only
      val path = uri.substring(0, uri.length - 1)
      // transform paths that now end with :*/ to :* (which matches optional trailing slashes)
      if (path.endsWith(":*/")) {
        path.substring(0, path.length - 1)
      } else {
        path
      }
    } else uri
  }
}
