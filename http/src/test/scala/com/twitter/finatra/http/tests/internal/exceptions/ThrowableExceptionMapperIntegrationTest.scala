package com.twitter.finatra.http.tests.internal.exceptions

import com.twitter.finagle.http.{Request, Status}
import com.twitter.finagle.{CancelledRequestException, Failure}
import com.twitter.finatra.http.contexts.RouteInfo
import com.twitter.finatra.http.internal.exceptions._
import com.twitter.finatra.http.modules.{MessageBodyModule, MustacheModule}
import com.twitter.finatra.json.modules.FinatraJacksonModule
import com.twitter.inject.{Injector, IntegrationTest}
import com.twitter.inject.app.TestInjector
import com.twitter.inject.modules.InMemoryStatsReceiverModule

class ThrowableExceptionMapperIntegrationTest extends IntegrationTest {
  override val injector: Injector =
    TestInjector(
      MessageBodyModule,
      FinatraJacksonModule,
      MustacheModule,
      InMemoryStatsReceiverModule
    ).create

  test("ThrowableExceptionMapper#unwrap Failure") {
    val failureExceptionMapper = injector.instance[FailureExceptionMapper]
    val cancelledRequestExceptionMapper = injector.instance[CancelledRequestExceptionMapper]

    val failure = Failure.wrap(new CancelledRequestException)

    val e = intercept[CancelledRequestException] {
      failureExceptionMapper.toResponse(request, failure)
    }

    cancelledRequestExceptionMapper.toResponse(request, e).status should be(
      Status.ClientClosedRequest
    )
  }

  test("ThrowableExceptionMapper#unwrap nested Failure") {
    val failureExceptionMapper = injector.instance[FailureExceptionMapper]
    val cancelledRequestExceptionMapper = injector.instance[CancelledRequestExceptionMapper]

    val failure = Failure.wrap(Failure.wrap(new CancelledRequestException))

    val e = intercept[CancelledRequestException] {
      failureExceptionMapper.toResponse(request, failure).status
    }

    cancelledRequestExceptionMapper.toResponse(request, e).status should be(
      Status.ClientClosedRequest
    )
  }

  test("ThrowableExceptionMapper#handle Failure without cause") {
    val failureExceptionMapper = injector.instance[FailureExceptionMapper]

    val failure = Failure("NO REASON")
    failureExceptionMapper.toResponse(request, failure).status should be(Status.InternalServerError)
  }

  test("ThrowableExceptionMapper#handle pathological Failure (greater than MaxDepth)") {
    val failureExceptionMapper = injector.instance[FailureExceptionMapper]

    val failure =
      Failure(Failure(Failure(Failure(Failure(Failure(new CancelledRequestException()))))))
    failureExceptionMapper.toResponse(request, failure).status should be(Status.InternalServerError)
  }

  test("ThrowableExceptionMapper#handle 'unhandled' exception") {
    val throwableExceptionMapper = injector.instance[ThrowableExceptionMapper]

    throwableExceptionMapper.toResponse(request, new Throwable("NO REASON")).status should be(
      Status.InternalServerError
    )
  }

  def request: Request = {
    val request = Request()
    RouteInfo.set(request, RouteInfo("a", "/b"))
    request
  }
}
