package com.twitter.finatra

import test.ShouldSpec

class FileSpec extends ShouldSpec {

  "looking up .json" should "recognize application/json" in {
    FileService.getContentType(".json") should equal("application/json")
  }

  "looking up .nonsense" should "default to application/octet-stream" in {
    FileService.getContentType(".nonsense") should equal("application/octet-stream")
  }

}
