package com.twitter.finatra

import test.SpecHelper

/* This test is used as the base for generating the
 README.markdown, all new generated apps, and the finatra_example repo
 */

class ExampleSpec extends SpecHelper {

  /* ###BEGIN_APP### */

  class ExampleApp extends Controller {

    get("/hello") { request =>
      render.plain("hello world").toFuture
    }

    get("/user/:username") { request =>
      val username = request.routeParams.getOrElse("username", "unknown")
      render.plain("hello " + username).toFuture
    }

  }

  val app = new ExampleApp

  /* ###END_APP### */


  /* ###BEGIN_SPEC### */

  "GET /hello" should "response with hello world" in {
    get("/hello")
    response.body should equal ("hello world")
    response.code should equal (200)
  }

  "GET /user/foo" should "response with foo" in {
    get("/user/foo")
    response.body should equal ("hello foo")
    response.code should equal (200)
  }

  /* ###END_SPEC### */
}
