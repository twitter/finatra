package com.twitter.finatra.mysql

import com.twitter.inject.TestMixin
import org.scalatest.TestSuite

/**
 * The [[EmbeddedMysqlServer]] instance and the clients created by referencing it will be cleaned
 * up after all tests have executed when this trait is used.
 *
 * Example usage:
 *
 * {{{
 *   class MyFooTest
 *     extends Test
 *     with TestLifecycle {
 *
 *     protected final val embeddedMysqlServer = EmbeddedMysqlServer()
 *
 *     test("MyFooTest#endpoint should do what it's supposed to do") {
 *       ...
 *     }
 *   }
 * }}}
 *
 */
trait MysqlTestLifecycle extends TestMixin { self: TestSuite =>

  /** Whether or not to run table truncation between each test invocation. */
  protected def truncateTablesBetweenTests: Boolean = false

  /** Tables to preserve when truncation occurs. */
  protected def preserveTables: Seq[String] = Seq.empty

  /**
   * The [[EmbeddedMysqlServer]] that will have its lifecycle managed for tests.
   * This value you should always be overridden as a `final val` in a concrete
   * test to ensure that the same instance is returned and managed. If this remains
   * a `def`, a new server will be created on each invocation and resources will leak.
   *
   * @example {{{
   *            class MyTest
   *              extends Test
   *              with TestLifecycle {
   *
   *              protected final val embeddedMysqlServer = EmbeddedMysqlServer()
   *              ...
   *            }
   * }}}
   */
  protected def embeddedMysqlServer: EmbeddedMysqlServer

  runAfterAll {
    embeddedMysqlServer.close()
  }

  override protected def afterEach(): Unit = {
    if (truncateTablesBetweenTests) embeddedMysqlServer.truncateAllTables(preserveTables)
    super.afterEach()
  }

}
