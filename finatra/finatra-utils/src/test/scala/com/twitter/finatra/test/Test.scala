package com.twitter.finatra.test

import com.twitter.util.{Await, Future}
import grizzled.slf4j.Logging
import org.apache.commons.io.IOUtils
import org.joda.time.{DateTime, DateTimeZone, Duration}
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.{Matchers, BeforeAndAfterAll, BeforeAndAfterEach, WordSpec}
import org.specs2.mock.Mockito

@RunWith(classOf[JUnitRunner])
abstract class Test
  extends WordSpec
  with Mockito
  with BeforeAndAfterAll
  with BeforeAndAfterEach
  with Matchers
  with Logging {

  protected def setPrivateField(obj: Object, fieldName: String, value: Object) {
    val field = obj.getClass.getDeclaredField(fieldName)
    field.setAccessible(true)
    field.set(obj, value)
  }

  protected def getPrivateField[T](obj: Object, fieldName: String) = {
    val field = obj.getClass.getDeclaredField(fieldName)
    field.setAccessible(true)
    field.get(obj).asInstanceOf[T]
  }

  protected def resetMocks(mocks: AnyRef*) {
    org.mockito.Mockito.reset(mocks: _*)
  }

  def banner(str: String) {
    Banner.banner(str)
  }

  def resourceAsString(resource: String) = {
    IOUtils.toString(
      getClass.getResourceAsStream(resource))
  }

  def sleep(duration: Duration, verbose: Boolean = false) {
    if (verbose) {
      println("Starting sleep for " + duration)
    }

    Thread.sleep(duration.getMillis)

    if (verbose) {
      println("Finished sleep for " + duration)
    }
  }

  def assertFuture[A](result: Future[A], expected: Future[A]) {
    val resultVal = Await.result(result)
    val expectedVal = Await.result(expected)
    resultVal should equal(expectedVal)
  }

  def assertFailedFuture[T <: Throwable : Manifest](result: Future[_]): T = {
    try {
      Await.result(result)
      fail("Expected exception " + manifest[T].erasure + " never thrown")
    } catch {
      case e: Throwable =>
        if (manifest[T].erasure.isAssignableFrom(e.getClass))
          e.asInstanceOf[T]
        else
          fail("Expected exception " + manifest[T].erasure + " but caught " + e)
    }
  }

  /** Current DateTime in UTC */
  def now = {
    DateTime.now.withZone(DateTimeZone.UTC)
  }

  def bytes(str: String) = {
    str.getBytes("UTF-8")
  }

  object TestException extends TestException

  class TestException extends Exception
}