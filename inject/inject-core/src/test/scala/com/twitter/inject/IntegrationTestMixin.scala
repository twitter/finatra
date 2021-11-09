package com.twitter.inject

import java.util.concurrent.ConcurrentLinkedQueue
import org.scalatest.Suite
import org.scalatest.SuiteMixin
import scala.util.control.NonFatal

/**
 * Testing trait which extends the [[com.twitter.inject.TestMixin]] to provide
 * utilities for Integration testing with a test-defined [[com.twitter.inject.Injector]].
 *
 * This trait is expected to be mixed with a class that extends a core Suite trait,
 * e.g., [[org.scalatest.funsuite.AnyFunSuite]].
 *
 * While you can use this mixin directly, it is recommended that users extend
 * the [[com.twitter.inject.IntegrationTest]] abstract class.
 *
 * @see [[com.twitter.inject.TestMixin]]
 */
trait IntegrationTestMixin extends SuiteMixin with TestMixin { this: Suite =>

  // while the test execution (beforeAll, afterAll, beforeEach, afterEach, test, etc)
  // is serial, we have no guarantees of where they will register afterAll functions, so
  // we attempt to provide thread safety.
  @volatile private[this] var afterAllStarted: Boolean = false
  private[this] val afterAllFns: ConcurrentLinkedQueue[() => Unit] =
    new ConcurrentLinkedQueue[() => Unit]()

  /* Protected */

  protected def injector: Injector

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
