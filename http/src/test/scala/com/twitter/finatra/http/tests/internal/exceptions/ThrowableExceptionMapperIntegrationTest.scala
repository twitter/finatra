package com.twitter.finatra.http.tests.internal.exceptions

import com.twitter.finagle.http.{Request, Status}
import com.twitter.finagle.{CancelledRequestException, Failure}
import com.twitter.finatra.http.contexts.RouteInfo
import com.twitter.finatra.http.internal.exceptions._
import com.twitter.finatra.http.modules.{MessageBodyModule, MustacheModule}
import com.twitter.finatra.json.modules.FinatraJacksonModule
import com.twitter.inject.WordSpecIntegrationTest
import com.twitter.inject.app.TestInjector
import com.twitter.inject.modules.InMemoryStatsReceiverModule

class ThrowableExceptionMapperIntegrationTest extends WordSpecIntegrationTest {
  override val injector = TestInjector(
    MessageBodyModule,
    FinatraJacksonModule,
    MustacheModule,
    InMemoryStatsReceiverModule)

  "ThrowableExceptionMapper" should {
    "unwrap Failure" in {
      val failureExceptionMapper = injector.instance[FailureExceptionMapper]
      val cancelledRequestExceptionMapper = injector.instance[CancelledRequestExceptionMapper]

      val failure = Failure(new CancelledRequestException)

      val e = intercept[CancelledRequestException] {
        failureExceptionMapper.toResponse(request, failure)
      }

      cancelledRequestExceptionMapper.toResponse(request, e).status should be(Status.ClientClosedRequest)
    }

    "unwrap nested Failure" in {
      val failureExceptionMapper = injector.instance[FailureExceptionMapper]
      val cancelledRequestExceptionMapper = injector.instance[CancelledRequestExceptionMapper]

      val failure = Failure(Failure(new CancelledRequestException))

      val e = intercept[CancelledRequestException] {
        failureExceptionMapper.toResponse(request, failure).status
      }

      cancelledRequestExceptionMapper.toResponse(request, e).status should be(Status.ClientClosedRequest)
    }

    "handle Failure without cause" in {
      val failureExceptionMapper = injector.instance[FailureExceptionMapper]

      val failure = Failure("NO REASON")
      failureExceptionMapper.toResponse(request, failure).status should be(Status.InternalServerError)
    }

    "handle pathological Failure (greater than MaxDepth)" in {
      val failureExceptionMapper = injector.instance[FailureExceptionMapper]

      val failure = Failure(Failure(Failure(Failure(Failure(Failure(new CancelledRequestException()))))))
      failureExceptionMapper.toResponse(request, failure).status should be(Status.InternalServerError)
    }

    "handle 'unhandled' exception" in {
      val throwableExceptionMapper = injector.instance[ThrowableExceptionMapper]

      throwableExceptionMapper.toResponse(request, new Throwable("NO REASON")).status should be(Status.InternalServerError)
    }
  }

  def request: Request = {
    val request = Request()
    RouteInfo.set(request, RouteInfo("a", "/b"))
    request
  }
}
