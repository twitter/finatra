package com.twitter.finatra

import com.twitter.finatra.test.ShouldSpec

class FileResolverSpec extends ShouldSpec {

  "FileResolverSpec" should "detect a directory" in {
    FileResolver.hasLocalDirectory("public/components") should be(true)
  }
}
