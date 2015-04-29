package com.twitter.finatra.http.internal.exceptions

import com.twitter.finagle.http.{Request, Response, Status}
import com.twitter.finatra.http.exceptions.{DefaultExceptionMapper, ExceptionMapper}
import com.twitter.finatra.http.response.SimpleResponse
import com.twitter.inject.Test
import com.twitter.inject.app.TestInjector
import org.jboss.netty.handler.codec.http.HttpResponseStatus
import org.specs2.mock.Mockito

class ExceptionManagerTest extends Test with Mockito {

  def newExceptionManager =
    new ExceptionManager(TestInjector(), TestDefaultExceptionMapper)

  val exceptionManager = newExceptionManager
  exceptionManager.add[ForbiddenExceptionMapper]
  exceptionManager.add(new UnauthorizedExceptionMapper)
  exceptionManager.add[UnauthorizedException1Mapper]

  def testException(e: Throwable, status: HttpResponseStatus) {
    val request = mock[Request]
    val response = exceptionManager.toResponse(request, e)
    response.status should equal(status)
  }

  "map exceptions to mappers installed with Guice" in {
    testException(new ForbiddenException, Status.Forbidden)
  }

  "map exceptions to mappers installed manually" in {
    testException(new UnauthorizedException, Status.Unauthorized)
  }

  "map subclass exceptions to parent class mappers" in {
    testException(new ForbiddenException1, Status.Forbidden)
    testException(new ForbiddenException2, Status.Forbidden)
  }

  "map exceptions to mappers of most specific class" in {
    testException(new UnauthorizedException1, Status.NotFound)
  }

  "fall back to default mapper" in {
    testException(new UnregisteredException, Status.InternalServerError)
  }

  "throw an IllegalStateException if exception mapped twice" in {
    val exceptionManager = newExceptionManager
    exceptionManager.add[ForbiddenExceptionMapper]
    intercept[IllegalStateException] {
      exceptionManager.add[ForbiddenExceptionMapper]
    }
  }

  "replace an exception mapper" in {
    val exceptionManager = newExceptionManager
    exceptionManager.add[ForbiddenExceptionMapper]
    exceptionManager.replace[ForbiddenIsOkExceptionMapper]
    val request = mock[Request]
    val response = exceptionManager.toResponse(request, new ForbiddenException)
    response.status should equal(Status.Ok)
  }

  "replace the default exception mapper" in {
    val exceptionManager = newExceptionManager
    exceptionManager.replace[EverythingIsFineMapper]
    val request = mock[Request]
    val response = exceptionManager.toResponse(request, new Exception)
    response.status should equal(Status.Ok)
  }
}

class UnregisteredException extends Exception
class ForbiddenException extends Exception
class ForbiddenException1 extends ForbiddenException
class ForbiddenException2 extends ForbiddenException1
class UnauthorizedException extends Exception
class UnauthorizedException1 extends UnauthorizedException

object TestDefaultExceptionMapper extends DefaultExceptionMapper {
  def toResponse(request: Request, throwable: Throwable): Response = {
    new SimpleResponse(Status.InternalServerError)
  }
}

class ForbiddenExceptionMapper extends ExceptionMapper[ForbiddenException] {
  def toResponse(request: Request, throwable: ForbiddenException): Response =
    new SimpleResponse(Status.Forbidden)
}

class ForbiddenIsOkExceptionMapper extends ExceptionMapper[ForbiddenException] {
  def toResponse(request: Request, throwable: ForbiddenException): Response =
    new SimpleResponse(Status.Ok)
}

class UnauthorizedExceptionMapper extends ExceptionMapper[UnauthorizedException] {
  def toResponse(request: Request, throwable: UnauthorizedException): Response =
    new SimpleResponse(Status.Unauthorized)
}

class UnauthorizedException1Mapper extends ExceptionMapper[UnauthorizedException1] {
  def toResponse(request: Request, throwable: UnauthorizedException1): Response =
    new SimpleResponse(Status.NotFound)
}

class EverythingIsFineMapper extends ExceptionMapper[Throwable] {
  def toResponse(request: Request, throwable: Throwable): Response =
    new SimpleResponse(Status.Ok)
}
