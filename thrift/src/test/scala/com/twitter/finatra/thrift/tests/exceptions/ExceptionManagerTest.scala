package com.twitter.finatra.thrift.tests.exceptions

import com.twitter.finagle.stats.InMemoryStatsReceiver
import com.twitter.finatra.thrift.exceptions.{ExceptionManager, ExceptionMapper}
import com.twitter.inject.Test
import com.twitter.inject.app.TestInjector
import com.twitter.util.{Await, Future}

class ExceptionManagerTest extends Test {

  def newExceptionManager =
    new ExceptionManager(TestInjector().create, new InMemoryStatsReceiver)

  val exceptionManager: ExceptionManager = newExceptionManager

  exceptionManager.add[RootExceptionMapper]
  exceptionManager.add[TypeOneExceptionMapper]
  exceptionManager.add[TypeTwoExceptionMapper]
  exceptionManager.add[FirstExceptionMapper]
  exceptionManager.add[SecondExceptionMapper]

  def testException(
    e: Throwable,
    mapped: String,
    manager: ExceptionManager = exceptionManager
  ): Unit = {
    Await.result(manager.handleException[String](e)) should equal(mapped)
  }

  test("map subclass exceptions to parent class mappers") {
    testException(new Throwable, "rootException")
    testException(new UnregisteredException, "rootException")
    testException(new TypeOneException, "typeOneException")
    testException(new TypeTwoException, "typeTwoException")
    testException(new TypeThreeException, "typeOneException")
  }

  test("map exceptions to last registered mapper") {
    testException(new ExceptionForDupMapper, "second, overridden!")
  }
}

class RootExceptionMapper extends ExceptionMapper[Throwable, String] {
  def handleException(throwable: Throwable): Future[String] = {
    Future.value("rootException")
  }
}

class TypeOneException extends Exception
class TypeTwoException extends TypeOneException
class TypeThreeException extends TypeOneException
class UnregisteredException extends Exception
class ExceptionForDupMapper extends Exception

class TypeOneExceptionMapper extends ExceptionMapper[TypeOneException, String] {
  def handleException(throwable: TypeOneException): Future[String] = {
    Future.value("typeOneException")
  }
}

class TypeTwoExceptionMapper extends ExceptionMapper[TypeTwoException, String] {
  def handleException(throwable: TypeTwoException): Future[String] = {
    Future.value("typeTwoException")
  }
}

class FirstExceptionMapper extends ExceptionMapper[ExceptionForDupMapper, String] {
  def handleException(throwable: ExceptionForDupMapper): Future[String] = {
    Future.value("first, win!")
  }
}

class SecondExceptionMapper extends ExceptionMapper[ExceptionForDupMapper, String] {
  def handleException(throwable: ExceptionForDupMapper): Future[String] = {
    Future.value("second, overridden!")
  }
}
