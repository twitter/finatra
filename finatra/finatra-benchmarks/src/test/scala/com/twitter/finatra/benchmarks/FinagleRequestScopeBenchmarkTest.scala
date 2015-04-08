package com.twitter.finatra.benchmarks

import com.twitter.inject.Test

class FinagleRequestScopeBenchmarkTest extends Test {

  "test" in {
    val benchmark = new FinagleRequestScopeBenchmark()
    benchmark.timeServiceWithRequestScopeFilter()
  }
}
