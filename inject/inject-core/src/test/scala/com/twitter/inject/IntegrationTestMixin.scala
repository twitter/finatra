package com.twitter.inject

import org.scalatest.{Suite, SuiteMixin}

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

  /* Protected */

  protected def injector: Injector
}
