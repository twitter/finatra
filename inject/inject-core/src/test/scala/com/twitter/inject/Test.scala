package com.twitter.inject

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.funsuite.AnyFunSuite

/**
 * Extensible abstract test class which extends [[org.scalatest.funsuite.AnyFunSuite]] and
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
 * @see [[org.scalatest.funsuite.AnyFunSuite AnyFunSuite]]
 * @see [[com.twitter.inject.TestMixin Finatra TestMixin]]
 */
@RunWith(classOf[JUnitRunner])
abstract class Test extends AnyFunSuite with TestMixin
