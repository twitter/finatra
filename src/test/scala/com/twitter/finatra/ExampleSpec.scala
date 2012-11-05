package com.twitter.finatra

import test.SpecHelper

/* This test is used as the base for generating the
 README.markdown and the template for the app generator
 */

class ExampleSpec extends SpecHelper {

  /* ###BEGIN_APP### */

  class ExampleApp extends Controller {
    get("/") { request =>
      render.plain("ok").toFuture
    }
  }

  val app = new ExampleApp

  /* ###END_APP### */


  /* ###BEGIN_SPEC### */

  "GET /" should "respond 200" in {
    get("/")
    response.body should equal ("ok")
    response.code should equal (200)
  }

  /* ###END_SPEC### */
}
