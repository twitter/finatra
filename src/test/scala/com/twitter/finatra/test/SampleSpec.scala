package com.twitter.finatra.test

import org.scalatest.matchers.ShouldMatchers
import org.scalatest.FlatSpec
import com.twitter.finatra.Controller

class SampleSpec extends FlatSpec with ShouldMatchers with TestHelper {

  class SampleController extends Controller {
    get("/testing") {
      request => render.body("hello world").status(200).toFuture
    }
  }

  "TestHelper" should "allow us to use separate controllers per test" in {
    val app: MockApp = usingController(new SampleController)

    // When
    app.get("/testing")

    // Then
    app.response.code should be(200)
    app.response.body should be("hello world")
  }
}
