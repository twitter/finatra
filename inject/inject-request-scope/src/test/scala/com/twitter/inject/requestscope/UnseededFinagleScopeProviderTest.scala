package com.twitter.inject.requestscope

import com.twitter.inject.Test

class UnseededFinagleScopeProviderTest extends Test {
  test("expect exception") {
    intercept[IllegalStateException] {
      new UnseededFinagleScopeProvider[String].get
    }
  }
}
