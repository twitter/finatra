package com.twitter.inject

import com.twitter.util.{Await, Future}
import java.util.TimeZone
import org.apache.commons.io.IOUtils
import org.joda.time.{DateTimeZone, Duration}
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, Matchers, WordSpec}

@RunWith(classOf[JUnitRunner])
abstract class Test
  extends WordSpec
  with BeforeAndAfterAll
  with BeforeAndAfterEach
  with Matchers
  with Logging {

  /* Constructor */

  setUtcTimeZone()

  /* Protected */

  protected def setUtcTimeZone() = {
    DateTimeZone.setDefault(DateTimeZone.UTC)
    TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
  }

  @deprecated("Use com.twitter.inject.Mockito#reset", "since 2-22-2015")
  protected def resetMocks(mocks: AnyRef*) {
    for (mock <- mocks) {
      trace("Resetting " + mock)
      org.mockito.Mockito.reset(mock)
    }
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

  protected def assertFutureValue[A](result: Future[A], expected: A) {
    val resultVal = Await.result(result)
    val expectedVal = Await.result(Future.value(expected))
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
}
