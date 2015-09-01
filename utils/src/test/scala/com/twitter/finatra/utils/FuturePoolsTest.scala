package com.twitter.finatra.utils

import com.twitter.inject.Test
import com.twitter.util.Await

class FuturePoolsTest extends Test {

  "named bounded" in {
    val pool = FuturePools.fixedPool("myBoundedPool", 1)
    val futureResult = pool {
      "hi"
    }
    Await.result(futureResult) should equal("hi")
    pool.executor.shutdownNow()
  }

  "named unbounded" in {
    val pool = FuturePools.unboundedPool("myUnboundedPool")
    val futureResult = pool {
      "hi"
    }
    Await.result(futureResult) should equal("hi")
    pool.executor.shutdownNow()
  }
}
