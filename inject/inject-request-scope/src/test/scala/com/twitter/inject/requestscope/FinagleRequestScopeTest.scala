package com.twitter.inject.requestscope

import com.google.inject.{OutOfScopeException, Key}
import com.twitter.inject.Test

class FinagleRequestScopeTest extends Test {

  test("seed string") {
    val scope = new FinagleRequestScope()
    scope.toString should be("FinagleRequestScope")
    scope.let {
      scope.seed[String]("abc")

      val unseededProvider = new UnseededFinagleScopeProvider[String]
      val provider = scope.scope(Key.get(classOf[String]), unseededProvider)
      provider.toString should include("UnseededFinagleScopeProvider")
      provider.get should be("abc")
    }
  }

  test("Use provider after scope exited") {
    val scope = new FinagleRequestScope()
    val unseededProvider = new UnseededFinagleScopeProvider[String]
    val provider = scope.scope(Key.get(classOf[String]), unseededProvider)

    intercept[OutOfScopeException] {
      println(provider.get)
    }
  }

  test("get scope for unseeded value") {
    val scope = new FinagleRequestScope()
    scope.let {

      val unseededProvider = new UnseededFinagleScopeProvider[String]
      val provider = scope.scope(Key.get(classOf[String]), unseededProvider)

      intercept[IllegalStateException] {
        provider.get
      }
    }
  }

  test("seed string before scope started") {
    val scope = new FinagleRequestScope()
    intercept[OutOfScopeException] {
      scope.seed[String]("abc")
    }
  }

  test("seed same object twice") {
    val scope = new FinagleRequestScope()
    scope.let {
      scope.seed[String]("abc")
      intercept[AssertionError] {
        scope.seed[String]("abc")
      }
    }
  }
}
