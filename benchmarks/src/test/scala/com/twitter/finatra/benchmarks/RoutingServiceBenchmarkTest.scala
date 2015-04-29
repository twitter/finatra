package com.twitter.finatra.benchmarks

import com.twitter.inject.Test

class RoutingServiceBenchmarkTest extends Test {

  "test" in {
    val benchmark = new RoutingServiceBenchmark()
    benchmark.testRoutingController()
  }
}
