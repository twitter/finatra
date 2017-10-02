package com.twitter.inject.server

import com.twitter.inject.Test

/**
 * Extensible abstract test class which uses the [[org.scalatest.FunSuite]] ScalaTest
 * style and mixes in the [[com.twitter.inject.server.FeatureTestMixin]] trait.
 *
 * Example usage:
 *
 * {{{
 *   class MyFooTest
 *     extends FeatureTest
 *     with Mockito {
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
 * @see [[org.scalatest.FunSuite FunSuite]]
 * @see [[com.twitter.inject.Test Finatra Test Class]]
 * @see [[com.twitter.inject.server.FeatureTestMixin Finatra FeatureTestMixin]]
 */
abstract class FeatureTest
  extends Test
  with FeatureTestMixin
