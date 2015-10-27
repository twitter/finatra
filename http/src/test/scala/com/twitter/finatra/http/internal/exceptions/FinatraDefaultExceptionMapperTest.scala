package com.twitter.finatra.http.internal.exceptions

import com.twitter.finagle.http.{Request, Status}
import com.twitter.finagle.{CancelledRequestException, Failure}
import com.twitter.finatra.http.modules.{MessageBodyModule, MustacheModule}
import com.twitter.finatra.json.modules.FinatraJacksonModule
import com.twitter.inject.Test
import com.twitter.inject.app.TestInjector

class FinatraDefaultExceptionMapperTest extends Test {

  val exceptionMapper =
    TestInjector(MessageBodyModule, FinatraJacksonModule, MustacheModule).instance[FinatraDefaultExceptionMapper]

  "FinatraDefaultExceptionMapper" should {
    "unwrap Failure" in {
      val failure = Failure(new CancelledRequestException)
      val request = Request()
      exceptionMapper.toResponse(request, failure).status should be(Status.ClientClosedRequest)
    }

    "unwrap nested Failure" in {
      val failure = Failure(Failure(new CancelledRequestException))
      val request = Request()
      exceptionMapper.toResponse(request, failure).status should be(Status.ClientClosedRequest)
    }

    "handle Failure without cause" in {
      val failure = Failure("no reason")
      val request = Request()
      exceptionMapper.toResponse(request, failure).status should be(Status.InternalServerError)
    }

    "handle pathological Failure" in {
      val failure = Failure(Failure(Failure(Failure(Failure(Failure(new CancelledRequestException()))))))
      val request = Request()
      exceptionMapper.toResponse(request, failure).status should be(Status.InternalServerError)
    }
  }
}
