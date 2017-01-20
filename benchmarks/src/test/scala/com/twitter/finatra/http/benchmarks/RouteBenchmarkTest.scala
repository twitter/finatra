package com.twitter.finatra.http.benchmarks

import com.twitter.inject.WordSpecTest

class RouteBenchmarkTest extends WordSpecTest {

  "test" in {
    val benchmark = new RouteBenchmark()
    benchmark.testRoute()
    benchmark.testRouteWithPathParams()
  }
}
