package com.twitter.inject.tests

import com.twitter.conversions.time._
import com.twitter.inject.{Test, WhenReadyMixin}
import com.twitter.util.Future
import org.scalatest.concurrent.ScalaFutures._

class WhenReadyMixinTest extends Test with WhenReadyMixin {
  test("should be able to use ScalaTest's whenReady with a Twitter Future") {
    whenReady(Future.apply("Test it")) { result =>
      result should be("Test it")
    }
  }

  test(
    "should be able to use a twitter Duration for the timeout"
  ) {
    whenReady(Future.apply("Test it"), timeout = 5.seconds) { result =>
      result should be("Test it")
    }
  }

  test(
    "should be able to use a twitter Duration for the interval"
  ) {
    whenReady(Future.apply("Test it"), interval = 10.milliseconds) { result =>
      result should be("Test it")
    }
  }
}
