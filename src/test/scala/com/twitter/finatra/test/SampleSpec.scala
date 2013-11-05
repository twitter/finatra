package com.twitter.finatra.test

import org.scalatest.matchers.ShouldMatchers
import org.scalatest.FlatSpec
import com.twitter.finatra.Controller

class SampleSpec extends FlatSpec with ShouldMatchers {

  class SampleController extends Controller {
    get("/testing") {
      request => render.body("hello world").status(200).toFuture
    }
  }

  "Sample Use Case" should "allow us to instantiate separate controller for each test" in {
    val app: MockApp = MockApp(new SampleController)

    // When
    val response = app.get("/testing")

    // Then
    response.code should be(200)
    response.body should be("hello world")
  }
}