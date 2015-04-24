package com.twitter.finatra.benchmarks

import com.twitter.inject.Test

class RouteBenchmarkTest extends Test {

  "test" in {
    val benchmark = new RouteBenchmark()
    benchmark.testRoute()
    benchmark.testRouteWithPathParams()
  }
}
