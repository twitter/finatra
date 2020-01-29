package com.twitter.finatra.json.benchmarks

import com.twitter.inject.Test

class JsonBenchmarkTest extends Test {

  test("json serde") {
    val benchmark = new JsonBenchmark()
    benchmark.withCaseClassDeserializer()
    benchmark.withoutCaseClassDeserializer()
  }
}
