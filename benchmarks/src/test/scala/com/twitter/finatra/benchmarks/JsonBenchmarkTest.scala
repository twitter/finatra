package com.twitter.finatra.benchmarks

import com.twitter.inject.Test

class JsonBenchmarkTest extends Test {

  "test" in {
    val benchmark = new JsonBenchmark()
    benchmark.finatraCustomCaseClassDeserializer()
    benchmark.jacksonScalaModuleCaseClassDeserializer()
  }
}
