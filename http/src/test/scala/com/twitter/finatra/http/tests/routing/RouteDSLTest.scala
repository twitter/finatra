package com.twitter.finatra.http.tests.routing

import com.twitter.finagle.http.{Request, Response}
import com.twitter.finagle.{Filter, Service, SimpleFilter}
import com.twitter.finatra.http.{FilteredDSL, PrefixedDSL, RouteDSL}
import com.twitter.inject.Test
import com.twitter.inject.app.TestInjector
import com.twitter.util.{Await, Future}

// For testing injection
class NormalTestFilter extends TestFilter("request" -> "true", "response" -> "true")

class TestFilter(
  appendToRequestMap: (String, String),
  appendToResponseMap: (String, String)
) extends SimpleFilter[Request, Response] {

  override def apply(request: Request, service: Service[Request, Response]): Future[Response] = {
    request.headerMap.add(appendToRequestMap._1, appendToRequestMap._2)
    service(request).map(response => {
      response.headerMap.add(appendToResponseMap._1, appendToResponseMap._2)
      response
    })
  }
}

class RouteDSLTest extends Test {
  private val identityService =
    Service.mk[Request, Response](request => Future.value(Response(request)))

  private val injector = TestInjector().create

  test("RouteDSL") {
    val routeDSL = new RouteDSL {}

    // We verify that calling the default buildFilter function returns an identity filter
    routeDSL.context.buildFilter(null) shouldEqual Filter.identity
    routeDSL.context.buildFilter(injector) shouldEqual Filter.identity
  }

  test("RouteDSL fails when route is not prefixed with leading slash") {
    intercept[IllegalArgumentException] {
      new RouteDSL {
        get("foo") { _: Request => // <---- this should fail
          "bar"
        }

        post("/foo") { _: Request =>
          "another bar"
        }
      }
    }
  }

  // Simple test to verify that the build filter is set appropriately in a new FilteredDSL
  test("FilteredDSL sets build filter") {
    val routeDSL = new FilteredDSL[NormalTestFilter]()

    val request = Request()
    // Verify that we changed the default
    val filter1 = routeDSL.context.buildFilter(injector)
    val filter2 = new NormalTestFilter()
    val response1 = Await.result(filter1(request, identityService))
    val response2 = Await.result(filter2(request, identityService))

    response1.headerMap shouldEqual response2.headerMap
  }

  test("FilteredDSL composes build filters") {
    val routeDSL = new FilteredDSL[NormalTestFilter]()
      .filter(new TestFilter("request2" -> "yes", "response2" -> "yes"))

    val request = Request()

    // Verify that we changed the default
    val filter1 = routeDSL.context.buildFilter(injector)
    val filter2 = new NormalTestFilter() andThen new TestFilter(
      "request2" -> "yes",
      "response2" -> "yes"
    )

    val response1 = Await.result(filter1(request, identityService))
    val response2 = Await.result(filter2(request, identityService))

    response1.headerMap shouldEqual response2.headerMap
  }

  test("PrefixedDSL should update the context prefix") {
    val routeDSL = new PrefixedDSL("/v1")

    routeDSL.context.prefix shouldEqual "/v1"
    routeDSL.context.buildFilter(injector) shouldEqual Filter.identity
  }

  test("PrefixedDSL should fail with no leading slash") {
    intercept[IllegalArgumentException] {
      new PrefixedDSL("v1")
    }
  }

  test("PrefixedDSL should chain to more prefixes") {
    val routeDSL = new PrefixedDSL("/v1").prefix("/api")

    routeDSL.context.prefix shouldEqual "/v1/api"
  }

  test("PrefixedDSL should work fine with a trailing slash and chain to more prefixes") {
    val routeDSL = new PrefixedDSL("/v1/").prefix("/api")

    routeDSL.context.prefix shouldEqual "/v1/api"
  }

  test("PrefixedDSL should work fine with a trailing slash and chain to more prefixes with trailing slashes") {
    val routeDSL = new PrefixedDSL("/v1/").prefix("/api/")

    routeDSL.context.prefix shouldEqual "/v1/api"
  }

  test("PrefixedDSL should fail when chained to more prefixes without leading slashes") {
    intercept[IllegalArgumentException  ] {
      new PrefixedDSL("/v1").prefix("api")
    }
  }

  test("PrefixedDSL should chain to FilteredDSL") {
    val routeDSL = new PrefixedDSL("/v1").filter[NormalTestFilter]

    routeDSL.context.prefix shouldEqual "/v1"

    val request = Request()
    val filter = routeDSL.context.buildFilter(injector)
    val response = Await.result(filter(request, identityService))

    response.headerMap("response") shouldEqual "true"
  }
}
