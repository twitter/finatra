package com.twitter.finatra.http.benchmarks

import com.twitter.inject.Test

class RoutingServiceBenchmarkTest extends Test {

  test("routing service") {
    val benchmark = new RoutingServiceBenchmark()
    benchmark.timeOldLastConstant()
    benchmark.timeOldLastNonConstant()
  }
}
