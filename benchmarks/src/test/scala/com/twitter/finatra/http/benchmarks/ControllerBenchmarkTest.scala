package com.twitter.finatra.http.benchmarks

import com.twitter.inject.WordSpecTest

class ControllerBenchmarkTest extends WordSpecTest {

  val benchmark = new ControllerBenchmark

  "test plaintext" in {
    benchmark.plaintext()
  }

  "test json" in {
    benchmark.json()
  }
}
