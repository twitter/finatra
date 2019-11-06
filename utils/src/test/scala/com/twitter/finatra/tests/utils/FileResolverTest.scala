package com.twitter.finatra.tests.utils

import com.twitter.finatra.utils.FileResolver
import com.twitter.inject.Test

class FileResolverTest extends Test {

  test("file resolver#not allow both doc roots to be set") {
    intercept[java.lang.AssertionError] {
      new FileResolver(localDocRoot = "src/main/webapp", docRoot = "com/twitter/foo/bar")
    }
  }

}
