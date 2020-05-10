package com.twitter.finatra.http.tests.exceptions

import com.twitter.finagle.http.{Request, Response, Status}
import com.twitter.finagle.stats.InMemoryStatsReceiver
import com.twitter.finatra.http.exceptions.{
  ExceptionManager,
  ExceptionMapper,
  ExceptionMapperCollection
}
import com.twitter.finatra.http.response.SimpleResponse
import com.twitter.finatra.httpclient.RequestBuilder
import com.twitter.inject.app.TestInjector
import com.twitter.inject.Test
import scala.util.Random

class ExceptionManagerTest extends Test {

  def newExceptionManager =
    new ExceptionManager(TestInjector().create, new InMemoryStatsReceiver)

  lazy val collectionExceptionManager =
    new ExceptionManager(TestInjector().create, new InMemoryStatsReceiver)

  def randomUri: String = {
    val version = s"${Random.alphanumeric.take(1).mkString}.${Random.alphanumeric.take(1).mkString}"
    val pathPart1 = Random.alphanumeric.take(5).mkString.toLowerCase()
    val pathPart2 = Random.alphanumeric.take(5).mkString.toLowerCase()
    s"/$version/$pathPart1/resource/$pathPart2"
  }

  val exceptionManager: ExceptionManager = newExceptionManager
  exceptionManager.add[TestRootExceptionMapper]
  exceptionManager.add[ForbiddenExceptionMapper]
  exceptionManager.add(new UnauthorizedExceptionMapper)
  exceptionManager.add[UnauthorizedException1Mapper]
  exceptionManager.add[FirstExceptionMapper]
  exceptionManager.add[SecondExceptionMapper]
  exceptionManager.add[RaiseInnerExceptionMapper]

  val exceptionMapperCollection: ExceptionMapperCollection = new ExceptionMapperCollection {
    add[TestRootExceptionMapper]
    add[ForbiddenExceptionMapper]
    add[UnauthorizedExceptionMapper]
    add[UnauthorizedException1Mapper]
    add[FirstExceptionMapper]
    add[SecondExceptionMapper]
    add[RaiseInnerExceptionMapper]
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

  test("map exceptions to mappers installed by type") {
    testException(new ForbiddenException, Status.Forbidden)
    testException(new ForbiddenException, Status.Forbidden, collectionExceptionManager)
  }

  test("map exceptions to mappers installed manually") {
    testException(new UnauthorizedException, Status.Unauthorized)
    testException(new UnauthorizedException, Status.Unauthorized, collectionExceptionManager)
  }

  test("map subclass exceptions to parent class mappers") {
    testException(new ForbiddenException1, Status.Forbidden)
    testException(new ForbiddenException1, Status.Forbidden, collectionExceptionManager)
    testException(new ForbiddenException2, Status.Forbidden)
    testException(new ForbiddenException2, Status.Forbidden, collectionExceptionManager)
  }

  test("map exceptions to mappers of most specific class") {
    testException(new UnauthorizedException1, Status.NotFound)
    testException(new UnauthorizedException1, Status.NotFound, collectionExceptionManager)
  }

  test("fall back to default mapper") {
    testException(new UnregisteredException, Status.InternalServerError)
    testException(new UnregisteredException, Status.InternalServerError, collectionExceptionManager)
  }

  test("map exceptions to last registered mapper") {
    testException(new ExceptionForDupMapper, Status.BadRequest)
    testException(new ExceptionForDupMapper, Status.BadRequest, collectionExceptionManager)
  }

  test("catch exceptions thrown by the mapper") {
    testException(RaiseInnerException(new UnregisteredException), Status.InternalServerError)
    testException(
      RaiseInnerException(new UnregisteredException),
      Status.InternalServerError,
      collectionExceptionManager)
  }
}

class UnregisteredException extends Exception
class ForbiddenException extends Exception
class ForbiddenException1 extends ForbiddenException
class ForbiddenException2 extends ForbiddenException1
class UnauthorizedException extends Exception
class UnauthorizedException1 extends UnauthorizedException
class ExceptionForDupMapper extends Exception
case class RaiseInnerException(cause: Exception) extends Exception

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

class FirstExceptionMapper extends ExceptionMapper[ExceptionForDupMapper] {
  def toResponse(request: Request, throwable: ExceptionForDupMapper): Response = {
    SimpleResponse(Status.Accepted)
  }
}

class SecondExceptionMapper extends ExceptionMapper[ExceptionForDupMapper] {
  def toResponse(request: Request, throwable: ExceptionForDupMapper): Response = {
    SimpleResponse(Status.BadRequest)
  }
}

class RaiseInnerExceptionMapper extends ExceptionMapper[RaiseInnerException] {
  def toResponse(request: Request, throwable: RaiseInnerException): Response = {
    throw throwable.cause
  }
}
