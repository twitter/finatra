package com.twitter.finatra

import com.twitter.finagle.Filter
import com.twitter.util.Future
import org.jboss.netty.handler.codec.http.HttpMethod
import collection.mutable.ListBuffer

// TODO maybe (Request => Option[Map[_, _]] (route parameters map)?
trait Matcher extends (Request => Option[Request]) {
  def andThen(next: Matcher): Matcher = {
    val outer = this
    new Matcher {
      def apply(request: Request) = outer.apply(request).flatMap {
        r => next(r)
      }
    }
  }
}

class HttpMethodMatcher(method: HttpMethod) extends Matcher {
  def apply(request: Request) = if (request.getMethod() == method) Some(request) else None
}

object HttpMethodMatcher {
  def apply(method: HttpMethod) = new HttpMethodMatcher(method)
}

class PathPatternMatcher(pattern: PathPattern) extends Matcher {
  def apply(request: Request) = {
    val matchResult = pattern(request.path.split('?').head)
    matchResult.map {
      thematch =>
        thematch.foreach(xs => extractParams(request, xs))
        request
    }

  }

  private[this] def extractParams(request: Request, xs: (_, _)) = {
    request.routeParams += (xs._1.toString -> xs._2.asInstanceOf[ListBuffer[String]].head.toString)
  }

}

object PathPatternMatcher {
  def sinatraPattern(path: String) = new PathPatternMatcher(SinatraPathPatternParser(path))
}

trait FilteredController {
  type Server = Request => Future[Response]

  var staticFilter: Filter[Request, Response, Request, Response] = Filter.identity
  var routingFilter: Filter[Request, Response, Request, Response] = Filter.identity
  var errorFilter: Filter[Request, Response, Request, Response] = Filter.identity

  def render = new Response

  def get(path: String)(server: Server): Unit = route(HttpMethodMatcher(HttpMethod.GET).andThen(PathPatternMatcher.sinatraPattern(path)))(server)

  def delete(path: String)(server: Server): Unit = route(HttpMethodMatcher(HttpMethod.DELETE).andThen(PathPatternMatcher.sinatraPattern(path)))(server)

  def post(path: String)(server: Server): Unit = route(HttpMethodMatcher(HttpMethod.POST).andThen(PathPatternMatcher.sinatraPattern(path)))(server)

  def put(path: String)(server: Server): Unit = route(HttpMethodMatcher(HttpMethod.PUT).andThen(PathPatternMatcher.sinatraPattern(path)))(server)

  def head(path: String)(server: Server): Unit = route(HttpMethodMatcher(HttpMethod.HEAD).andThen(PathPatternMatcher.sinatraPattern(path)))(server)

  def patch(path: String)(server: Server): Unit = route(HttpMethodMatcher(HttpMethod.PATCH).andThen(PathPatternMatcher.sinatraPattern(path)))(server)

  def error(handler: PartialFunction[Throwable, Future[Response]]): Unit = {
    errorFilter = errorFilter.andThen(ErrorHandlerFilter(handler))
  }

  def route(matcher: Request => Option[Request])(server: Server): Unit = route(Router(matcher, server))

  def route(router: Router): Unit = {
    routingFilter = routingFilter.andThen(RouterFilter(router))
  }

  def asFilter = staticFilter.andThen(errorFilter).andThen(routingFilter)

}

