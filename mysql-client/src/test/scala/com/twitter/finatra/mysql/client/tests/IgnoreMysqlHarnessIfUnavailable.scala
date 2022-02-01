package com.twitter.finatra.mysql.client.tests

import com.twitter.finatra.mysql.EmbeddedMysqlServer
import com.twitter.inject.Test
import org.scalactic.source.Position
import org.scalatest.Tag

/** Skip any tests tagged with [[MysqlHarnessTag]] if the test harness is not available */
trait IgnoreMysqlHarnessIfUnavailable extends Test {

  private[this] lazy val isHarnessDefined: Boolean =
    EmbeddedMysqlServer.newBuilder().withLazyStart.newServer().testHarnessExists()

  abstract override protected def test(
    testName: String,
    testTags: Tag*
  )(
    testFun: => Any /* Assertion */
  )(
    implicit pos: Position
  ): Unit =
    if (testTags.contains(MysqlHarnessTag) && !isHarnessDefined) {
      warn(s"No MySQL test harness was found, skipping '$testName' test")
      ignore(testName, testTags: _*)(testFun)(pos)
    } else super.test(testName, testTags: _*)(testFun)(pos)

}
