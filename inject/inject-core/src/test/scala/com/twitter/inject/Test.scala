package com.twitter.inject

import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner

/**
 * Extensible abstract test class which extends [[org.scalatest.FunSuite]] and
 * mixes in the [[com.twitter.inject.TestMixin]] trait.
 *
 * Example usage:
 *
 * {{{
 *   class MyFooTest
 *     extends Test
 *     with Mockito {
 *
 *     test("Foo#method should do what it's supposed to do") {
 *       ...
 *     }
 *   }
 * }}}
 *
 * @see [[org.scalatest.FunSuite FunSuite]]
 * @see [[com.twitter.inject.TestMixin Finatra TestMixin]]
 */
 @RunWith(classOf[JUnitRunner])
abstract class Test
  extends FunSuite
  with TestMixin
