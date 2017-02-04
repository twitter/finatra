package com.twitter.finatra.http.benchmarks

import com.twitter.inject.WordSpecTest

class RoutingServiceBenchmarkTest extends WordSpecTest {

  "test" in {
    val benchmark = new RoutingServiceBenchmark()
    benchmark.timeOldLastConstant()
    benchmark.timeOldLastNonConstant()
  }
}
