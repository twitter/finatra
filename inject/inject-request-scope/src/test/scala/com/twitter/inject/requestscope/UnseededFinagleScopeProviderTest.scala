package com.twitter.inject.requestscope

import com.twitter.inject.WordSpecTest


class UnseededFinagleScopeProviderTest extends WordSpecTest {
  "expect exception" in {
    intercept[IllegalStateException] {
      new UnseededFinagleScopeProvider[String].get
    }
  }
}
