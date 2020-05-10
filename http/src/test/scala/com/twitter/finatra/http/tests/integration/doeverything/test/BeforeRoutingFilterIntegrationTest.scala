package com.twitter.finatra.http.tests.integration.doeverything.test

import com.twitter.finagle.{Service, SimpleFilter}
import com.twitter.finagle.http.{Request, Response, Status}
import com.twitter.finatra.http.contexts.RouteInfo
import com.twitter.finatra.http.filters.ExceptionMappingFilter
import com.twitter.finatra.http.routing.HttpRouter
import com.twitter.finatra.http.{Controller, EmbeddedHttpServer, HttpServer}
import com.twitter.inject.{Mockito, Test}
import com.twitter.util.Future
import org.scalatest.Matchers

class BeforeRoutingFilterIntegrationTest extends Test with Mockito {

  test("Failing beforeRouting = true has exceptions correctly mapped by ExceptionManager") {
    val server = new EmbeddedHttpServer(
      twitterServer = new HttpServer {
        override def configureHttp(router: HttpRouter): Unit = {
          router
            .filter[ExceptionMappingFilter[Request]](beforeRouting = true)
            .filter[FailingFilter](beforeRouting = true)
            .add[NullController]
        }
      },
      disableTestLogging = true)

    try {
      server.httpGet("/a_route_uri", andExpect = Status.InternalServerError)

      server.inMemoryStats.counters.assert("route/a_route_uri/GET/status/500/mapped/Exception", 1)
      server.inMemoryStats.counters
        .assert("service/failure/Internal/Unhandled/java.lang.Exception", 1)
    } finally {
      server.close()
    }
  }

  test("RouteInfo is present in a beforeRouting = true filter") {
    val server = new EmbeddedHttpServer(
      twitterServer = new HttpServer {
        override def configureHttp(router: HttpRouter): Unit = {
          router
            .filter[ExceptionMappingFilter[Request]](beforeRouting = true)
            .filter(
              new CheckRouteInfoOnResponseFilter("/failing", "failing_route"),
              beforeRouting = true)
            .add[FailingController]
        }
      },
      disableTestLogging = true)

    try {
      server.httpGet("/failing", andExpect = Status.InternalServerError)

      server.inMemoryStats.counters.assert("route/failing/GET/status/500/mapped/Exception", 1)
      server.inMemoryStats.counters
        .assert("service/failure/Internal/Unhandled/java.lang.Exception", 1)
    } finally {
      server.close()
    }
  }

}

class FailingFilter extends SimpleFilter[Request, Response] with Matchers {
  def apply(request: Request, svc: Service[Request, Response]): Future[Response] = {
    throw new Exception("FORCED FILTER EXCEPTION")
  }
}

class NullController extends Controller {
  get("/foo", name = "my_foo") { _: Request =>
    response.ok
  }
}

class CheckRouteInfoOnResponseFilter(path: String, name: String)
    extends SimpleFilter[Request, Response]
    with Matchers {
  def apply(request: Request, svc: Service[Request, Response]): Future[Response] = {
    svc(request).map { response =>
      val info = RouteInfo(request).get
      info.path should be(path)
      info.name should be(name)
      response
    }
  }
}

class FailingController extends Controller {
  get("/failing", name = "failing_route") { _: Request =>
    throw new Exception("FORCED CONTROLLER EXCEPTION")
  }
}
