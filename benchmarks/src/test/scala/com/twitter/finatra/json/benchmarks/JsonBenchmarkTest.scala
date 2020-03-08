package com.twitter.finatra.json.benchmarks

import com.twitter.inject.Test

class JsonBenchmarkTest extends Test {
  private[this] val benchmark = new JsonBenchmark()

  test("json serde") {
    benchmark.withCaseClassDeserializer()
    benchmark.withoutCaseClassDeserializer()
  }
}
