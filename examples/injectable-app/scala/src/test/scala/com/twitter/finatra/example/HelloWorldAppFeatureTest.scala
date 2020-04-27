package com.twitter.finatra.example

import com.twitter.inject.Test
import com.twitter.inject.app.EmbeddedApp

class HelloWorldAppFeatureTest extends Test {

  // Note this is purposely a def which creates (and thus runs) a new
  // instance in every test case. It is important to not reuse a stateful
  // application between test cases as each test case runs the application
  // again and reuse can lead to non-deterministic tests.
  def app = new EmbeddedApp(new SampleApp)
  def app(underlying: SampleApp) = new EmbeddedApp(underlying)

  test("SampleApp#print help") {
    // help always terminates with a non-zero exit-code.
    intercept[Exception] {
      app.main("help" -> "true")
    }
  }

  test("SampleApp#run") {
    val underlying = new SampleApp

    app(underlying).main("username" -> "jdoe")

    val queue: Seq[Int] = underlying.getQueue
    queue should equal(Seq(1, 2, 3, 4, 5, 6))
  }
}
