package com.twitter.inject.tests

import com.twitter.inject.{Logging, Test}
import com.twitter.util.Future

/**
 * This is in inject-core as in sbt we cannot add a dependency
 * on inject-core to inject-slf4j) since inject-core depends on inject-slf4j
 * and would thus cause a cyclic dependency.
 *
 * The fact that inject-slf4j would only need the inject-core:test-jar does
 * not matter as sbt doesn't consider the ivy configurations orthogonally
 * when computing the cycle thus we still end up with a cycle.
 */
class LoggingTest extends Test with Logging {

  test("Logging") {
    debug("a")
    warn("a")
    info("a")
    trace("a")

    debugResult("%s") { "a" }
    warnResult("%s") { "a" }
    infoResult("%s") { "a" }
    errorResult("%s") { "a" }
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
}
