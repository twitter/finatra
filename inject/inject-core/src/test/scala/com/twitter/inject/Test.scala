package com.twitter.inject

import org.scalatest.funsuite.AnyFunSuite

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
 * @see [[org.scalatest.funsuite.AnyFunSuite FunSuite]]
 * @see [[com.twitter.inject.TestMixin Finatra TestMixin]]
 */
abstract class Test
  extends AnyFunSuite
  with TestMixin
