package com.twitter.finatra.http.tests.internal.exceptions

import com.twitter.finagle.http.{Request, Response, Status}
import com.twitter.finagle.stats.InMemoryStatsReceiver
import com.twitter.finatra.http.exceptions.{ExceptionMapperCollection, ExceptionManager, ExceptionMapper}
import com.twitter.finatra.http.response.SimpleResponse
import com.twitter.finatra.httpclient.RequestBuilder
import com.twitter.inject.WordSpecTest
import com.twitter.inject.app.TestInjector
import org.apache.commons.lang.RandomStringUtils
import org.specs2.mock.Mockito

class ExceptionManagerTest extends WordSpecTest with Mockito {

  def newExceptionManager =
    new ExceptionManager(
      TestInjector(),
      new InMemoryStatsReceiver)

  lazy val collectionExceptionManager =
    new ExceptionManager(
      TestInjector(),
      new InMemoryStatsReceiver)

  def randomUri = {
    val version = s"${RandomStringUtils.randomNumeric(1)}.${RandomStringUtils.randomNumeric(1)}"
    val pathPart1 = RandomStringUtils.randomAlphabetic(5).toLowerCase()
    val pathPart2 = RandomStringUtils.randomAlphabetic(5).toLowerCase()
    s"/$version/$pathPart1/resource/$pathPart2"
  }

  val exceptionManager = newExceptionManager
  exceptionManager.add[TestRootExceptionMapper]
  exceptionManager.add[ForbiddenExceptionMapper]
  exceptionManager.add(new UnauthorizedExceptionMapper)
  exceptionManager.add[UnauthorizedException1Mapper]

  val exceptionMapperCollection = new ExceptionMapperCollection {
    add[TestRootExceptionMapper]
    add[ForbiddenExceptionMapper]
    add[UnauthorizedExceptionMapper]
    add[UnauthorizedException1Mapper]
  }
  collectionExceptionManager
      .add(exceptionMapperCollection)

  def testException(
    e: Throwable,
    status: Status,
    manager: ExceptionManager = exceptionManager
  ): Unit = {
    val request = RequestBuilder.get(randomUri)
    val response = manager.toResponse(request, e)
    response.status should equal(status)
  }

  "map exceptions to mappers installed by type" in {
    testException(new ForbiddenException, Status.Forbidden)
    testException(new ForbiddenException, Status.Forbidden, collectionExceptionManager)
  }

  "map exceptions to mappers installed manually" in {
    testException(new UnauthorizedException, Status.Unauthorized)
    testException(new UnauthorizedException, Status.Unauthorized, collectionExceptionManager)
  }

  "map subclass exceptions to parent class mappers" in {
    testException(new ForbiddenException1, Status.Forbidden)
    testException(new ForbiddenException1, Status.Forbidden, collectionExceptionManager)
    testException(new ForbiddenException2, Status.Forbidden)
    testException(new ForbiddenException2, Status.Forbidden, collectionExceptionManager)
  }

  "map exceptions to mappers of most specific class" in {
    testException(new UnauthorizedException1, Status.NotFound)
    testException(new UnauthorizedException1, Status.NotFound, collectionExceptionManager)
  }

  "fall back to default mapper" in {
    testException(new UnregisteredException, Status.InternalServerError)
    testException(new UnregisteredException, Status.InternalServerError, collectionExceptionManager)
  }
}

class UnregisteredException extends Exception
class ForbiddenException extends Exception
class ForbiddenException1 extends ForbiddenException
class ForbiddenException2 extends ForbiddenException1
class UnauthorizedException extends Exception
class UnauthorizedException1 extends UnauthorizedException

class TestRootExceptionMapper extends ExceptionMapper[Throwable] {
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
