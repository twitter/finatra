package com.twitter.finatra.json.benchmarks

import com.twitter.inject.Test

class JsonBenchmarkTest extends Test {

  "test" in {
    val benchmark = new JsonBenchmark()
    benchmark.finatraCustomCaseClassDeserializer()
    benchmark.jacksonScalaModuleCaseClassDeserializer()
  }
}
