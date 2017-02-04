package com.twitter.finatra.json.benchmarks

import com.twitter.inject.WordSpecTest

class JsonBenchmarkTest extends WordSpecTest {

  "test" in {
    val benchmark = new JsonBenchmark()
    benchmark.finatraCustomCaseClassDeserializer()
    benchmark.jacksonScalaModuleCaseClassDeserializer()
  }
}
