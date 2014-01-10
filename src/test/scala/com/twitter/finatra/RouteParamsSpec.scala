package com.twitter.finatra

import com.twitter.finatra.test.{FlatSpecHelper, SpecHelper}

class RouteParamsSpec extends FlatSpecHelper {

  class ExampleApp extends Controller {
    get("/:foo") { request =>
      val decoded = request.routeParams.get("foo").get
      render.plain(decoded).toFuture
    }
  }

  val server = new FinatraServer
  server.register(new ExampleApp)

  "Response" should "contain decoded params" in {
    get("/hello%3Aworld")
    response.body should equal("hello:world")
  }


}
