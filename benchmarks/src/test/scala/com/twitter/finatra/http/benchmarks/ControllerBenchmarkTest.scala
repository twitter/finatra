package com.twitter.finatra.http.benchmarks

import com.twitter.inject.Test

class ControllerBenchmarkTest extends Test {

  val benchmark = new ControllerBenchmark

  test("test plaintext") {
    benchmark.plaintext()
  }

  test("test json") {
    benchmark.json()
  }
}
