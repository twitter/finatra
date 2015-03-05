package com.twitter.inject.requestscope

import org.scalatest.FunSuite

class UnseededFinagleScopeProviderTest extends FunSuite {
  test("expect exception") {
    intercept[IllegalStateException] {
      new UnseededFinagleScopeProvider[String].get
    }
  }
}
