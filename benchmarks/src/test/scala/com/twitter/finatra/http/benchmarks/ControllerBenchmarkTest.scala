package com.twitter.finatra.http.benchmarks

import com.twitter.inject.Test

class ControllerBenchmarkTest extends Test {

  val benchmark = new ControllerBenchmark

  "test plaintext" in {
    benchmark.plaintext()
  }

  "test json" in {
    benchmark.json()
  }
}
