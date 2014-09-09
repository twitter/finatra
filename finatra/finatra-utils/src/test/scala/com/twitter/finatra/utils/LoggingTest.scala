package com.twitter.finatra.utils

import com.twitter.finatra.test.Test
import com.twitter.util.Future

class LoggingTest extends Test with Logging {

  "logging" in {
    debug("a")
    warn("a")
    info("a")
    trace("a")

    debugResult("%s") {"a"}
    warnResult("%s") {"a"}
    infoResult("%s") {"a"}
    errorResult("%s") {"a"}
    debugFutureResult("%s") {
      Future("a")
    }
  }
}
