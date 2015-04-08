package com.twitter.inject.requestscope

import com.google.inject.{OutOfScopeException, Key}
import com.twitter.inject.Test

class FinagleRequestScopeTest extends Test {

  "seed string" in {
    val scope = new FinagleRequestScope()
    scope.toString should be("FinagleRequestScope")
    scope.enter()

    scope.seed[String]("abc")

    val unseededProvider = new UnseededFinagleScopeProvider[String]
    val provider = scope.scope(Key.get(classOf[String]), unseededProvider)
    provider.toString should include("UnseededFinagleScopeProvider")
    provider.get should be("abc")
    scope.exit()
  }

  "Use provider after scope exited" in {
    val scope = new FinagleRequestScope()
    scope.enter()
    scope.seed[String]("abc")

    val unseededProvider = new UnseededFinagleScopeProvider[String]
    val provider = scope.scope(Key.get(classOf[String]), unseededProvider)
    scope.exit()

    intercept[OutOfScopeException] {
      println(provider.get)
    }
  }

  "get scope for unseeded value" in {
    val scope = new FinagleRequestScope()
    scope.enter()

    val unseededProvider = new UnseededFinagleScopeProvider[String]
    val provider = scope.scope(Key.get(classOf[String]), unseededProvider)

    intercept[IllegalStateException] {
      provider.get
    }
  }

  "seed string before scope started" in {
    val scope = new FinagleRequestScope()
    intercept[OutOfScopeException] {
      scope.seed[String]("abc")
    }
  }

  "seed same object twice" in {
    val scope = new FinagleRequestScope()
    scope.enter()

    scope.seed[String]("abc")
    intercept[AssertionError] {
      scope.seed[String]("abc")
    }
    scope.exit()
  }
}
