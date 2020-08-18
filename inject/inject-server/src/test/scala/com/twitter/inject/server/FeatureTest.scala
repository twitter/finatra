package com.twitter.inject.server

import com.twitter.inject.Test

/**
 * Extensible abstract test class which uses the [[org.scalatest.funsuite.AnyFunSuite]] ScalaTest
 * style and mixes in the [[com.twitter.inject.server.FeatureTestMixin]] trait.
 *
 * Example usage:
 *
 * {{{
 *   class MyFooTest
 *     extends FeatureTest {
 *
 *     override val server = new EmbeddedTwitterServer(
 *       twitterServer = new MyTwitterServer)
 *
 *     test("TestServer#endpoint should do what it's supposed to do") {
 *       ...
 *     }
 *   }
 * }}}
 *
 * @see [[org.scalatest.funsuite.AnyFunSuite AnyFunSuite]]
 * @see [[com.twitter.inject.Test Finatra Test Class]]
 * @see [[com.twitter.inject.server.FeatureTestMixin Finatra FeatureTestMixin]]
 */
abstract class FeatureTest extends Test with FeatureTestMixin
