package com.twitter.finatra.http.integration.routing

import com.twitter.finagle.{Service, SimpleFilter}
import com.twitter.finagle.http.{Response, Request, Status}
import com.twitter.finatra.http.contexts.RouteInfo
import com.twitter.finatra.http.routing.HttpRouter
import com.twitter.finatra.http.test.EmbeddedHttpServer
import com.twitter.finatra.http.{Controller, HttpServer}
import com.twitter.inject.Test
import com.twitter.util.Future
import org.scalatest.Matchers

class RoutingIntegrationTest extends Test {

  "Provide RouteInfo in a global filter" in {
    val server = new EmbeddedHttpServer(
      twitterServer = new HttpServer {
        override def configureHttp(router: HttpRouter) {
          router
            .filter[CheckRouteInfoFilter]
            .add[NullController]
        }
      })

    try {
      server.httpGet(
        "/foo",
        andExpect = Status.Ok)
    } finally {
      server.close()
    }
  }

  "Support global filter beforeRouting true" in {
    val server = new EmbeddedHttpServer(
      twitterServer = new HttpServer {
        override def configureHttp(router: HttpRouter) {
          router
            .filter[NoRouteInfoFilter](beforeRouting = true)
            .filter[CheckRouteInfoFilter]
            .add[NullController]
        }
      })

    try {
      server.httpGet(
        "/foo",
        andExpect = Status.Ok)
    } finally {
      server.close()
    }
  }

  "Support global filter beforeRouting false" in {
    // NOTE: you SHOULD NOT need to do this, just
    // call the no-argument version of router#filter
    val server = new EmbeddedHttpServer(
      twitterServer = new HttpServer {
        override def configureHttp(router: HttpRouter) {
          router
            .filter[CheckRouteInfoFilter](beforeRouting = false)
            .add[NullController]
        }
      })

    try {
      server.httpGet(
        "/foo",
        andExpect = Status.Ok)
    } finally {
      server.close()
    }
  }

  "Provide RouteInfo in a per-controller filter" in {
    val server = new EmbeddedHttpServer(
      twitterServer = new HttpServer {
        override def configureHttp(router: HttpRouter) {
          router
            .add[CheckRouteInfoFilter, NullController]
        }
      })

    try {
      server.httpGet(
        "/foo",
        andExpect = Status.Ok)
    } finally {
      server.close()
    }
  }

  "Fail if adding a filter after adding a controller" in {
    val server = new EmbeddedHttpServer(
      twitterServer = new HttpServer {
        override def configureHttp(router: HttpRouter) {
          router
            .add[NullController]
            .filter[CheckRouteInfoFilter]
        }
      })

    try {
      intercept[AssertionError] {
        server.httpGet("/foo",
          andExpect = Status.Ok)
      }
    } finally {
      server.close()
    }
  }
}

class NoRouteInfoFilter extends SimpleFilter[Request, Response] with Matchers {
  def apply(request: Request, svc: Service[Request, Response]): Future[Response] = {
    val info = RouteInfo(request)
    info should be(None)
    svc(request)
  }
}

class CheckRouteInfoFilter extends SimpleFilter[Request, Response] with Matchers {
  def apply(request: Request, svc: Service[Request, Response]): Future[Response] = {
    val info = RouteInfo(request).get
    info.path should be("/foo")
    info.name should be("my_foo")
    svc(request)
  }
}

class NullController extends Controller {
  get("/foo", name = "my_foo") { request: Request =>
    response.ok
  }
}