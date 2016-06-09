package com.twitter.finatra.http.tests.internal.exceptions

import com.twitter.finagle.http.{Request, Status}
import com.twitter.finagle.{CancelledRequestException, Failure}
import com.twitter.finatra.http.contexts.RouteInfo
import com.twitter.finatra.http.internal.exceptions.FinatraDefaultExceptionMapper
import com.twitter.finatra.http.modules.{MessageBodyModule, MustacheModule}
import com.twitter.finatra.json.modules.FinatraJacksonModule
import com.twitter.inject.IntegrationTest
import com.twitter.inject.app.TestInjector
import com.twitter.inject.modules.InMemoryStatsReceiverModule

class FinatraDefaultExceptionMapperIntegrationTest extends IntegrationTest {
  override val injector = TestInjector(
    MessageBodyModule,
    FinatraJacksonModule,
    MustacheModule,
    InMemoryStatsReceiverModule)

  val exceptionMapper = injector.instance[FinatraDefaultExceptionMapper]

  "FinatraDefaultExceptionMapper" should {
    "unwrap Failure" in {
      val failure = Failure(new CancelledRequestException)
      exceptionMapper.toResponse(request, failure).status should be(Status.ClientClosedRequest)
    }

    "unwrap nested Failure" in {
      val failure = Failure(Failure(new CancelledRequestException))
      exceptionMapper.toResponse(request, failure).status should be(Status.ClientClosedRequest)
    }

    "handle Failure without cause" in {
      val failure = Failure("no reason")
      exceptionMapper.toResponse(request, failure).status should be(Status.InternalServerError)
    }

    "handle pathological Failure" in {
      val failure = Failure(Failure(Failure(Failure(Failure(Failure(new CancelledRequestException()))))))
      exceptionMapper.toResponse(request, failure).status should be(Status.InternalServerError)
    }
  }

  def request: Request = {
    val request = Request()
    RouteInfo.set(request, RouteInfo("a", "/b"))
    request
  }
}
