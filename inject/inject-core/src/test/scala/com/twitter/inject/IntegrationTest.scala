package com.twitter.inject

/**
 * Extensible abstract test class which uses the [[org.scalatest.funsuite.AnyFunSuite]] ScalaTest
 * style and mixes in the [[com.twitter.inject.IntegrationTestMixin]] trait.
 *
 * Example usage:
 *
 * {{{
 *   class MyFooTest
 *     extends IntegrationTest
 *     with Mockito {
 *
 *     override val injector =
 *       TestInjector(
 *         MyModule1,
 *         MyBarModule,
 *         FooModule)
 *       .create
 *
 *     test("Foo#method should do what it's supposed to do") {
 *       ...
 *     }
 *   }
 * }}}
 *
 * @see [[org.scalatest.funsuite.AnyFunSuite AnyFunSuite]]
 * @see [[com.twitter.inject.Test Finatra Test Class]]
 * @see [[com.twitter.inject.IntegrationTestMixin Finatra IntegrationTestMixin]]
 */
abstract class IntegrationTest extends Test with IntegrationTestMixin
