package com.twitter.finatra.http.tests.routing

import com.twitter.finagle.Service
import com.twitter.finagle.http.{Request, Response}
import com.twitter.finatra.http.routing.{HttpRouter, HttpWarmup, RoutesByType, Services}
import com.twitter.inject.Test
import com.twitter.util.jackson.ScalaObjectMapper
import com.twitter.util.mock.Mockito
import com.twitter.util.{Future, Throw}
import java.util.concurrent.atomic.AtomicInteger

class HttpWarmupTest extends Test with Mockito {
  private val mapper: ScalaObjectMapper = ScalaObjectMapper()
  private val requestsCreated: AtomicInteger = new AtomicInteger(0)
  private val requestsHandled: AtomicInteger = new AtomicInteger(0)
  private val doNotCallService: Service[Request, Response] =
    Service.mk(_ => Future.const(Throw(new IllegalStateException)))
  private val incrementingService: Service[Request, Response] = {
    Service.mk(req => {
      // Check header on the request
      assert(req.headerMap.get("Host").contains("127.0.0.1"))
      requestsHandled.incrementAndGet()
      Future.value(Response())
    })
  }

  override protected def beforeEach(): Unit = {
    super.beforeEach()
    requestsCreated.set(0)
    requestsHandled.set(0)
  }

  test("validate warmup creates correct number of request objects") {
    val httpRouter = mock[HttpRouter]
    httpRouter.services returns Services(
      mock[RoutesByType],
      adminService = doNotCallService,
      externalService = incrementingService)

    val warmup = new HttpWarmup(httpRouter, mapper)

    warmup.send(makeRequest(), times = 11)()
    assert(requestsCreated.get() == 11)
    assert(requestsHandled.get() == 11)
  }

  private def makeRequest(): Request = {
    requestsCreated.incrementAndGet()
    Request("/test")
  }
}
