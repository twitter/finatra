package com.twitter.finatra.http.benchmarks

import com.twitter.inject.WordSpecTest

class FinagleRequestScopeBenchmarkTest extends WordSpecTest {

  "test" in {
    val benchmark = new FinagleRequestScopeBenchmark()
    benchmark.timeServiceWithRequestScopeFilter()
  }
}
