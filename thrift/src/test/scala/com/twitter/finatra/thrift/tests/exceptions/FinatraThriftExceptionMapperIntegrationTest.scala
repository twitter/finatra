package com.twitter.finatra.thrift.tests.exceptions

import com.twitter.conversions.DurationOps._
import com.twitter.finagle.{CancelledRequestException, Failure, RequestTimeoutException}
import com.twitter.finatra.thrift.exceptions.FinatraThriftExceptionMapper
import com.twitter.finatra.thrift.thriftscala.{
  ClientError,
  NoClientIdError,
  ServerError,
  UnknownClientIdError
}
import com.twitter.finatra.thrift.thriftscala.ClientErrorCause.{BadRequest, RequestTimeout}
import com.twitter.finatra.thrift.thriftscala.ServerErrorCause.InternalServerError
import com.twitter.inject.app.TestInjector
import com.twitter.inject.{Injector, IntegrationTest}

class FinatraThriftExceptionMapperIntegrationTest extends IntegrationTest {
  override def injector: Injector = TestInjector().create

  val finatraThriftExceptionMapper = injector.instance[FinatraThriftExceptionMapper]

  test("FinatraThriftExceptionMapper#TimeoutException") {
    val timeoutException = new RequestTimeoutException(1.second, "timeoutException")
    val e = assertFailedFuture[ClientError] {
      finatraThriftExceptionMapper.handleException(timeoutException)
    }
    e.errorCause should equal(RequestTimeout)
  }

  test("FinatraThriftExceptionMapper#ClientError") {
    val clientError = new ClientError(BadRequest, "thriftException")
    val e = assertFailedFuture[ClientError] {
      finatraThriftExceptionMapper.handleException(clientError)
    }
    e.errorCause should equal(BadRequest)
  }

  test("FinatraThriftExceptionMapper#UnknownClientIdError") {
    val unknownClientIdError = new UnknownClientIdError("unknownClientIdError")
    val e = assertFailedFuture[UnknownClientIdError] {
      finatraThriftExceptionMapper.handleException(unknownClientIdError)
    }
    e.getMessage should equal("unknownClientIdError")
  }

  test("FinatraThriftExceptionMapper#NoClientIdError") {
    val noClientIdError = new NoClientIdError("noClientIdError")
    val e = assertFailedFuture[NoClientIdError] {
      finatraThriftExceptionMapper.handleException(noClientIdError)
    }
    e.getMessage should equal("noClientIdError")
  }

  test("FinatraThriftExceptionMapper#Failure") {
    val failure = Failure(new CancelledRequestException)
    val e = assertFailedFuture[ServerError] {
      finatraThriftExceptionMapper.handleException(failure)
    }
    e.getMessage should include("request cancelled")
    e.errorCause should equal(InternalServerError)
  }

  test("FinatraThriftExceptionMapper#UnhandledException") {
    val failure = new Exception("NO REASON")
    val e = assertFailedFuture[ServerError] {
      finatraThriftExceptionMapper.handleException(failure)
    }
    e.getMessage should equal("NO REASON")
    e.errorCause should equal(InternalServerError)
  }
}
