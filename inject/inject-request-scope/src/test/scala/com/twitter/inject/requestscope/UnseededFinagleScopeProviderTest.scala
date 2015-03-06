package com.twitter.inject.requestscope

import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class UnseededFinagleScopeProviderTest extends FunSuite {
  test("expect exception") {
    intercept[IllegalStateException] {
      new UnseededFinagleScopeProvider[String].get
    }
  }
}
