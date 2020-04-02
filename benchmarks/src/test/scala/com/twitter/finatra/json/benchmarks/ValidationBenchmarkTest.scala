package com.twitter.finatra.json.benchmarks
import com.twitter.inject.Test

class ValidationBenchmarkTest extends Test {
  private[this] val benchmark = new ValidationBenchmark()

  test("validate case class") {
    benchmark.withValidUser()
    benchmark.withInvalidUser()
  }

  test("validate nested case class") {
    benchmark.withNestedValidUser()
    benchmark.withNestedInvalidUser()
    benchmark.withNestedDuplicateUser()
  }
}
