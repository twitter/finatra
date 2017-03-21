package com.twitter.finatra

import com.twitter.util.Future
import com.twitter.finagle.{Service, SimpleFilter}

// TODO maybe delete this trait?
trait Router {

  def matches: Request => Option[Request]

  def process: Request => Future[Response]

}

object Router {
  def apply(matcher: Request => Option[Request], server: Request => Future[Response]) = new Router {
    def process = server

    def matches = matcher
  }
}

class RouterFilter(router: Router) extends SimpleFilter[Request, Response] {
  def apply(originalRequest: Request, service: Service[Request, Response]) =
    router.matches(originalRequest) match {
      case Some(request) => router.process(request)
      case None => service(originalRequest)
    }
}

object RouterFilter {
  def apply(router: Router) = new RouterFilter(router)
}

class ErrorHandlerFilter(handler: PartialFunction[Throwable, Future[Response]]) extends SimpleFilter[Request, Response] {
  def apply(originalRequest: Request, service: Service[Request, Response]) = service(originalRequest).rescue(handler)
}

object ErrorHandlerFilter {
  def apply(handler: PartialFunction[Throwable, Future[Response]]) = new ErrorHandlerFilter(handler)
}