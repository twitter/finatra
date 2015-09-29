package com.twitter.inject.tests

import com.twitter.inject.{Logger, Logging, Test}
import com.twitter.util.Future

class LoggingTest extends Test with Logging {

  "Logging" in {
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

    time("time %s ms") {
      1 + 2
    }

    intercept[RuntimeException] {
      time("error time %s ms") {
        throw new RuntimeException("oops")
      }
    }
  }

  "Logger" in {
    val log = Logger[LoggingTest]()
    log.info("Hi")
  }
}
