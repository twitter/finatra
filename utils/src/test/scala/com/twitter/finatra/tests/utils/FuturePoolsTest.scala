package com.twitter.finatra.tests.utils

import com.twitter.finatra.utils.FuturePools
import com.twitter.inject.Test
import com.twitter.util.Await

class FuturePoolsTest extends Test {

  test("named bounded") {
    val pool = FuturePools.fixedPool("myBoundedPool", 1)
    val futureResult = pool {
      "hi"
    }
    Await.result(futureResult) should equal("hi")
    pool.executor.shutdownNow()
  }

  test("named unbounded") {
    val pool = FuturePools.unboundedPool("myUnboundedPool")
    val futureResult = pool {
      "hi"
    }
    Await.result(futureResult) should equal("hi")
    pool.executor.shutdownNow()
  }
}
