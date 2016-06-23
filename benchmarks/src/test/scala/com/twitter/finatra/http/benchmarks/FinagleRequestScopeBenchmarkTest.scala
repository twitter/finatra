package com.twitter.finatra.http.benchmarks

import com.twitter.inject.Test

class FinagleRequestScopeBenchmarkTest extends Test {

  "test" in {
    val benchmark = new FinagleRequestScopeBenchmark()
    benchmark.timeServiceWithRequestScopeFilter()
  }
}
