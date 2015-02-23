package com.twitter.finatra.test

import com.twitter.finatra.utils.Logging
import com.twitter.util.{Await, Future}
import java.io.File
import java.util.TimeZone
import org.apache.commons.io.IOUtils
import org.joda.time.{DateTimeZone, Duration}
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, Matchers, WordSpec}
import scala.io.Source

@RunWith(classOf[JUnitRunner])
abstract class Test
  extends WordSpec
  with BeforeAndAfterAll
  with BeforeAndAfterEach
  with Matchers
  with Logging {

  /* Constructor */

  setTimeZone()

  /* Protected */

  protected def setTimeZone() = {
    DateTimeZone.setDefault(DateTimeZone.UTC)
    TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
  }

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
    for (mock <- mocks) {
      println("Resetting " + mock)
      org.mockito.Mockito.reset(mock)
    }
  }

  protected def banner(str: String) {
    Banner.banner(str)
  }

  protected def resourceAsString(resource: String) = {
    IOUtils.toString(
      getClass.getResourceAsStream(resource))
  }

  protected def sleep(duration: Duration, verbose: Boolean = false) {
    if (verbose) {
      println("Starting sleep for " + duration)
    }

    Thread.sleep(duration.getMillis)

    if (verbose) {
      println("Finished sleep for " + duration)
    }
  }

  protected def assertFuture[A](result: Future[A], expected: Future[A]) {
    val resultVal = Await.result(result)
    val expectedVal = Await.result(expected)
    resultVal should equal(expectedVal)
  }

  protected def assertFailedFuture[T <: Throwable : Manifest](result: Future[_]): T = {
    try {
      Await.result(result)
      fail("Expected exception " + manifest[T].runtimeClass + " never thrown")
    } catch {
      case e: Throwable =>
        if (manifest[T].runtimeClass.isAssignableFrom(e.getClass))
          e.asInstanceOf[T]
        else
          fail("Expected exception " + manifest[T].runtimeClass + " but caught " + e)
    }
  }

  protected def bytes(str: String) = {
    str.getBytes("UTF-8")
  }

  object TestException extends TestException

  class TestException extends Exception
}
