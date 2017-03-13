package com.twitter.finatra.http.benchmarks

import com.twitter.inject.Test

class RouteBenchmarkTest extends Test {

  test("routes") {
    val benchmark = new RouteBenchmark()
    benchmark.testRoute()
    benchmark.testRouteWithPathParams()
  }
}
