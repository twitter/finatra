package com.twitter.finatra.http.tests.integration.routing

import com.twitter.finagle.http.Method.Trace
import com.twitter.finagle.http._
import com.twitter.finagle.{Service, SimpleFilter}
import com.twitter.finatra.http.contexts.RouteInfo
import com.twitter.finatra.http.routing.HttpRouter
import com.twitter.finatra.http.EmbeddedHttpServer
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

  "Support any route for matching" in {
    val server = new EmbeddedHttpServer(
      twitterServer = new HttpServer {
        override def configureHttp(router: HttpRouter): Unit = {
          router.add[AmazingController]
        }
      }
    )

    try {
      server.httpGet(
        "/foo",
        andExpect = Status.Ok)

      server.httpGet(
        "/proxy/tweet/2838282683",
        andExpect = Status.Ok)

      server.httpGet(
        "/proxy/user/11221111222",
        andExpect = Status.Ok)

      server.httpPost(
        "/proxy/user/48483021",
        postBody = "",
        andExpect = Status.Ok)

      server.httpPost(
        "/proxy/tweet/48483021",
        postBody = "",
        andExpect = Status.Ok)

      server.httpPut(
        "/proxy/thingamagic/99898936",
        putBody = "",
        andExpect = Status.Ok)

      server.httpPost(
        "/foo",
        postBody = "",
        andExpect = Status.NotFound)

      server.httpRequest(
        Request(Trace, "/proxy/thingamagic/99898936"),
        andExpect = Status.MethodNotAllowed)
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

class AmazingController extends Controller {
  get("/foo", name = "my_foo") { request: Request =>
    response.ok
  }

  any("/proxy/:base/:*") { request: Request =>
    // This can be useful for proxying. We manually handle what methods
    // are supported; for the purpose of this example we *don't* support trace
    request.method match {
      case Method.Trace =>
        response.methodNotAllowed
      case _ =>
        response.ok(s"Sent proxy request for ${request.params("base")}")
    }

  }
}