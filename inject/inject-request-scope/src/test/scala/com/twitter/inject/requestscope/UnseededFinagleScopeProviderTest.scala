package com.twitter.inject.requestscope

import com.twitter.inject.Test


class UnseededFinagleScopeProviderTest extends Test {
  "expect exception" in {
    intercept[IllegalStateException] {
      new UnseededFinagleScopeProvider[String].get
    }
  }
}
