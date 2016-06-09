package com.twitter.finatra.http.tests.internal.exceptions

import com.twitter.finagle.http.{Request, Response, Status}
import com.twitter.finagle.stats.InMemoryStatsReceiver
import com.twitter.finatra.http.exceptions.{DefaultExceptionMapper, ExceptionManager, ExceptionMapper}
import com.twitter.finatra.http.response.SimpleResponse
import com.twitter.finatra.httpclient.RequestBuilder
import com.twitter.inject.Test
import com.twitter.inject.app.TestInjector
import org.apache.commons.lang.RandomStringUtils
import org.specs2.mock.Mockito

class ExceptionManagerTest extends Test with Mockito {

  def newExceptionManager =
    new ExceptionManager(
      TestInjector(),
      TestDefaultExceptionMapper,
      new InMemoryStatsReceiver)

  def randomUri = {
    val version = s"${RandomStringUtils.randomNumeric(1)}.${RandomStringUtils.randomNumeric(1)}"
    val pathPart1 = RandomStringUtils.randomAlphabetic(5).toLowerCase()
    val pathPart2 = RandomStringUtils.randomAlphabetic(5).toLowerCase()
    s"/$version/$pathPart1/resource/$pathPart2"
  }

  val exceptionManager = newExceptionManager
  exceptionManager.add[ForbiddenExceptionMapper]
  exceptionManager.add(new UnauthorizedExceptionMapper)
  exceptionManager.add[UnauthorizedException1Mapper]

  def testException(e: Throwable, status: Status) {
    val request = RequestBuilder.get(randomUri)
    val response = exceptionManager.toResponse(request, e)
    response.status should equal(status)
  }

  "map exceptions to mappers installed by type" in {
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
}

class UnregisteredException extends Exception
class ForbiddenException extends Exception
class ForbiddenException1 extends ForbiddenException
class ForbiddenException2 extends ForbiddenException1
class UnauthorizedException extends Exception
class UnauthorizedException1 extends UnauthorizedException

object TestDefaultExceptionMapper extends DefaultExceptionMapper {
  def toResponse(request: Request, throwable: Throwable): Response = {
    SimpleResponse(Status.InternalServerError)
  }
}

class ForbiddenExceptionMapper extends ExceptionMapper[ForbiddenException] {
  def toResponse(request: Request, throwable: ForbiddenException): Response =
    SimpleResponse(Status.Forbidden)
}

class UnauthorizedExceptionMapper extends ExceptionMapper[UnauthorizedException] {
  def toResponse(request: Request, throwable: UnauthorizedException): Response =
    SimpleResponse(Status.Unauthorized)
}

class UnauthorizedException1Mapper extends ExceptionMapper[UnauthorizedException1] {
  def toResponse(request: Request, throwable: UnauthorizedException1): Response =
    SimpleResponse(Status.NotFound)
}
