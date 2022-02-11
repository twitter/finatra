package com.twitter.inject

import com.twitter.util.Await
import com.twitter.util.Awaitable
import com.twitter.util.Duration
import com.twitter.util.ExecutorServiceFuturePool
import com.twitter.util.Future
import com.twitter.util.{logging => utl}
import java.nio.charset.{StandardCharsets => JChar}
import java.util.TimeZone
import com.twitter.io.StreamIO
import java.util.concurrent.ConcurrentLinkedQueue
import org.joda.time.DateTimeZone
import org.scalatest.BeforeAndAfterAll
import org.scalatest.BeforeAndAfterEach
import org.scalatest.Suite
import org.scalatest.SuiteMixin
import org.scalatest.matchers.should.Matchers
import scala.util.control.NonFatal

/**
 * Testing trait which provides the following stackable modification traits:
 *  - [[org.scalatest.BeforeAndAfterAll]]
 *  - [[org.scalatest.BeforeAndAfterEach]]
 *  - [[org.scalatest.matchers.should.Matchers]]
 *  - [[com.twitter.util.logging.Logging]]
 *
 * This trait is expected to be mixed with a class that extends a core Suite trait,
 * e.g., [[org.scalatest.funsuite.AnyFunSuite]].
 *
 * While you can use this mixin directly, it is recommended that users extend
 * the [[com.twitter.inject.Test]] abstract class.
 */
trait TestMixin
    extends SuiteMixin
    with BeforeAndAfterAll
    with BeforeAndAfterEach
    with Matchers
    with utl.Logging { this: Suite =>

  /* Constructor */

  setUtcTimeZone()

  // while the test execution (beforeAll, afterAll, beforeEach, afterEach, test, etc)
  // is serial, we have no guarantees of where they will register afterAll functions, so
  // we attempt to provide thread safety.
  @volatile private[this] var afterAllStarted: Boolean = false
  private[this] val afterAllFns: ConcurrentLinkedQueue[() => Unit] =
    new ConcurrentLinkedQueue[() => Unit]()

  /* Overrides */

  /**
   * When overriding, ensure that `super.afterAll()` is called.
   */
  override protected def afterAll(): Unit =
    try {
      super.afterAll()
    } finally {
      afterAllStarted = true
      // we drain the queue and call the exits, regardless of exceptions
      while (!afterAllFns.isEmpty) {
        executeFn(afterAllFns.poll())
      }

      try {
        pool.executor.shutdown()
      } catch {
        case t: Throwable =>
          println(
            s"Unable to shutdown ${"Test " + getClass.getSimpleName} future pool executor. $t")
          t.printStackTrace()
      }
    }

  /* Protected */

  /**
   * Logic that needs to be executed during the [[afterAll()]] block of tests. This method
   * allows for resources, such as clients or servers, to be cleaned up after test execution
   * without the need for multiple overrides to `afterAll` and calling `super`.
   *
   * @param f The logic to execute in the [[afterAll()]]
   */
  protected final def runAfterAll(f: => Unit): Unit =
    if (afterAllStarted) executeFn(() => f) // execute inline if we've started `afterAll`
    else afterAllFns.add(() => f) // otherwise we add it to the queue

  /**
   * An unbounded [[ExecutorServiceFuturePool]] available for use in testing.
   *
   * @note the resultant [[com.twitter.util.FuturePool]] will be given a name of this
   *       test class file prepended with "Test". See [[PoolUtils.newUnboundedPool(name)]].
   *
   * @see [[com.twitter.util.ExecutorServiceFuturePool]]
   * @see [[com.twitter.util.FuturePool]]
   */
  protected lazy val pool: ExecutorServiceFuturePool =
    PoolUtils.newUnboundedPool("Test " + getClass.getSimpleName)

  /**
   * The default timeout for all internal [[Await]] calls.
   *
   * @note the default value is 5 seconds.
   * @return a [[com.twitter.util.Duration]]
   *
   * @see [[com.twitter.util.Await]]
   */
  protected def defaultAwaitTimeout: Duration = Duration.fromSeconds(5)

  protected final def setUtcTimeZone(): Unit = {
    DateTimeZone.setDefault(DateTimeZone.UTC)
    TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
  }

  /**
   * Reads the resource identified by the given path as a String.
   *
   * @param resource the path to the resource tp load.
   * @return the loaded resource as a String.
   *
   * @see [[java.nio.charset.StandardCharsets.UTF_8]]
   */
  protected def resourceAsString(resource: String): String = {
    StreamIO.buffer(getClass.getResourceAsStream(resource)).toString(JChar.UTF_8.displayName)
  }

  /**
   * Awaits for a result from the given [[com.twitter.util.Future]].
   *
   * @param awaitable the [[com.twitter.util.Awaitable]] to await.
   * @tparam A the parameterized type of the given [[Awaitable]]. The returned value will be of this type.
   * @return the resultant [[A]] or a thrown Exception in the case of a failed execution.
   *
   * @see [[com.twitter.util.Await.result]]
   */
  protected def await[A](awaitable: Awaitable[A]): A =
    Await.result(awaitable, defaultAwaitTimeout)

  /**
   * Asserts the resultant value of two [[Future]] executions are equivalent
   * using [[org.scalatest.matchers.should.Matchers]].
   *
   * @param result the actual [[Future]]
   * @param expected the expected [[Future]]
   *
   * @see [[org.scalatest.matchers.should.Matchers]]
   * @see [[com.twitter.inject.TestMixin.await]]
   * @see [[com.twitter.inject.TestMixin.defaultAwaitTimeout]]
   */
  protected def assertFuture[A](result: Future[A], expected: Future[A]): Unit = {
    await(result) should equal(await(expected))
  }

  /**
   * Asserts the resultant value of the given [[Future]] is equivalent to the give
   * expected value using [[org.scalatest.matchers.should.Matchers]].
   *
   * @param result the actual [[Future]]
   * @param expected the expected value
   *
   * @see [[com.twitter.inject.TestMixin.await]]
   * @see [[com.twitter.inject.TestMixin.defaultAwaitTimeout]]
   */
  protected def assertFutureValue[A](result: Future[A], expected: A): Unit = {
    await(result) should equal(expected)
  }

  /**
   * Asserts that the given [[Future]] is a failed execution resulting in a [[Throwable]] of
   * type [[T]].
   *
   * @param result the actual [[Future]]
   * @tparam T the expected [[Throwable]] type
   * @return the resultant [[Throwable]]
   *
   * @see [[com.twitter.inject.TestMixin.await]]
   * @see [[com.twitter.inject.TestMixin.defaultAwaitTimeout]]
   */
  protected def assertFailedFuture[T <: Throwable: Manifest](result: Future[_]): T = {
    intercept[T] {
      await(result)
    }
  }

  /**
   * Encodes this the given String into a sequence of bytes using the
   * [[java.nio.charset.StandardCharsets.UTF_8]], storing the result into a
   * new byte array.
   *
   * @param str the String to encode.
   * @return the resultant byte array.
   *
   * @see [[java.lang.String#getBytes]]
   * @see [[java.nio.charset.StandardCharsets.UTF_8]]
   */
  protected def bytes(str: String): Array[Byte] = {
    str.getBytes(JChar.UTF_8)
  }

  // we always try/catch because we don't want non-fatal errors to fail tests in our `afterAll`
  private[this] def executeFn(f: () => Unit): Unit =
    try {
      f()
    } catch {
      case NonFatal(e) =>
        e.printStackTrace(System.err)
    }
}
