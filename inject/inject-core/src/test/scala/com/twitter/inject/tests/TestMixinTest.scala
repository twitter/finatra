package com.twitter.inject.tests

import com.twitter.inject.Test
import com.twitter.util.Future
import org.scalatest.exceptions.TestFailedException

class TestMixinTest extends Test {
  val illegalArgumentName = classOf[IllegalArgumentException].getName
  val illegalStateName = classOf[IllegalStateException].getName
  val exceptionName = classOf[Exception].getName

  test("assertFailedFuture can identify the appropriate exception") {
    val illegal = new IllegalArgumentException("boom!")
    val f = Future.exception(illegal)
    val actual = assertFailedFuture[IllegalArgumentException](f)
    assert(actual == illegal)
  }

  test("assertFailedFuture fails when it's not passed the right exception") {
    val illegal = new IllegalStateException("wrong boom!")
    val f = Future.exception(illegal)
    val exn = intercept[TestFailedException] {
      assertFailedFuture[IllegalArgumentException](f)
    }
    assert(
      exn.getMessage == s"Expected exception $illegalArgumentName to be thrown, but $illegalStateName was thrown")
  }

  test("assertFailedFuture fails when it's not passed a failed future") {
    val f = Future.value("no boom")
    val exn = intercept[TestFailedException] {
      assertFailedFuture[IllegalArgumentException](f)
    }
    assert(
      exn.getMessage == s"Expected exception $illegalArgumentName to be thrown, but no exception was thrown")
  }

  test(
    "assertFailedFuture still fails when it's not passed a failed future even when the exception would catch a `fail`") {
    val f = Future.value("no boom")
    val exn = intercept[TestFailedException] {
      assertFailedFuture[Exception](f)
    }
    assert(
      exn.getMessage == s"Expected exception $exceptionName to be thrown, but no exception was thrown")
  }
}
