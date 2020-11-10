package com.twitter.finatra.json.benchmarks

import com.twitter.finatra.StdBenchAnnotations
import com.twitter.finatra.validation.{
  MethodValidation,
  ValidationException,
  ValidationResult,
  Validator
}
import com.twitter.finatra.validation.constraints.{Min, NotEmpty}
import org.openjdk.jmh.annotations._

case class User(@NotEmpty id: String, name: String, @Min(18) age: Int)

case class Users(@NotEmpty users: Seq[User]) {
  @MethodValidation
  def uniqueUsers: ValidationResult =
    ValidationResult.validate(
      users.map(_.id).distinct.size == users.size,
      "user ids are not distinct.")
}

/**
 * ./sbt 'project benchmarks' 'jmh:run ValidationBenchmark'
 */
@State(Scope.Thread)
class ValidationBenchmark extends StdBenchAnnotations {

  val validator: Validator = Validator()
  val validUser: User = User("1234567", "jack", 21)
  val invalidUser: User = User("", "notJack", 13)
  val nestedValidUser: Users = Users(Seq(validUser))
  val nestedInvalidUser: Users = Users(Seq(invalidUser))
  val nestedDuplicateUser: Users = Users(Seq(validUser, validUser))

  @Benchmark
  def withValidUser(): Unit = {
    validator.verify(validUser)
  }

  @Benchmark
  def withInvalidUser(): Unit = {
    try {
      validator.verify(invalidUser)
    } catch {
      case _: ValidationException => // avoid throwing exceptions so the benchmark can finish
    }
  }

  @Benchmark
  def withNestedValidUser(): Unit = {
    validator.verify(nestedValidUser)
  }

  @Benchmark
  def withNestedInvalidUser(): Unit = {
    try {
      validator.verify(nestedInvalidUser)
    } catch {
      case _: ValidationException => // avoid throwing exceptions so the benchmark can finish
    }
  }

  @Benchmark
  def withNestedDuplicateUser(): Unit = {
    try {
      validator.verify(nestedDuplicateUser)
    } catch {
      case _: ValidationException => // avoid throwing exceptions so the benchmark can finish
    }
  }
}
