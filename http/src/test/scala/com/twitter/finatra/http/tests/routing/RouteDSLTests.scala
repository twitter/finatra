package com.twitter.finatra.http.tests.routing

import com.twitter.finagle.http.{Request, Response}
import com.twitter.finagle.{Filter, Service, SimpleFilter}
import com.twitter.finatra.http.{FilteredDSL, PrefixedDSL, RouteDSL}
import com.twitter.inject.Test
import com.twitter.inject.app.TestInjector
import com.twitter.util.{Await, Future}
import org.scalatest.OptionValues

// So we can test injection
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

class RouteDSLTests extends Test with OptionValues {
  val identityService = Service.mk[Request, Response](req => Future.value(Response(req)))

  test("RouteDSL") {
    val injector = TestInjector()
    val dsl = new RouteDSL {}

    // We verify that calling the default buildFilter function returns an identity filter
    dsl.context.buildFilter(null) shouldEqual Filter.identity
    dsl.context.buildFilter(injector) shouldEqual Filter.identity
  }

  // Simple test to verify that the build filter is set appropriately in a new FilteredDSL
  test("FilteredDSL sets build filter") {
    val injector = TestInjector()
    val dsl = new FilteredDSL[NormalTestFilter]()

    val req = Request()
    // Verify that we changed the default
    val filter1 = dsl.context.buildFilter(injector)
    val filter2 = new NormalTestFilter()
    val res1 = Await.result(filter1(req, identityService))
    val res2 = Await.result(filter2(req, identityService))

    res1.headerMap shouldEqual res2.headerMap
  }

  test("FilteredDSL composes build filters") {
    val injector = TestInjector()
    val dsl = new FilteredDSL[NormalTestFilter]().filter(new TestFilter("request2" -> "yes", "response2" -> "yes"))

    val req = Request()

    // Verify that we changed the default
    val filter1 = dsl.context.buildFilter(injector)
    val filter2 = new NormalTestFilter() andThen new TestFilter("request2" -> "yes", "response2" -> "yes")

    val res1 = Await.result(filter1(req, identityService))
    val res2 = Await.result(filter2(req, identityService))

    res1.headerMap shouldEqual res2.headerMap
  }

  test("PrefixedDSL should update the context prefix") {
    val injector = TestInjector()
    val dsl = new PrefixedDSL("v1")

    dsl.context.prefix shouldEqual "v1"
    dsl.context.buildFilter(injector) shouldEqual Filter.identity
  }

  test("PrefixedDSL should chain to more prefixes") {
    val dsl = new PrefixedDSL("v1").prefix("/api")

    dsl.context.prefix shouldEqual "v1/api"
  }

  test("PrefixedDSL should chain to FilteredDSL") {
    val injector = TestInjector()
    val dsl = new PrefixedDSL("v1").filter[NormalTestFilter]

    dsl.context.prefix shouldEqual "v1"

    val req = Request()
    val filter = dsl.context.buildFilter(injector)
    val res = Await.result(filter(req, identityService))

    res.headerMap("response") shouldEqual "true"
  }
}
