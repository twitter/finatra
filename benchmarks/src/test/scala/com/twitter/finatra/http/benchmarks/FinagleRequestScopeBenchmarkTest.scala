package com.twitter.finatra.http.benchmarks

import com.twitter.inject.Test

class FinagleRequestScopeBenchmarkTest extends Test {

  test("finagle request scope") {
    val benchmark = new FinagleRequestScopeBenchmark()
    benchmark.timeServiceWithRequestScopeFilter()
  }
}
