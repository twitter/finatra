/**
 * Copyright (C) 2012 Twitter Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
