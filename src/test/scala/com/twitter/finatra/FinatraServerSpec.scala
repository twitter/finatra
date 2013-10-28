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
package com.twitter.finatra.test

import com.twitter.finatra.{Controller, FinatraServer, AppService, LoggingFilter}
import com.twitter.finagle.Service
import com.twitter.finagle.http.{Request => FinagleRequest, Response => FinagleResponse}
import org.scalatest.matchers.ShouldMatchers


class TestApp extends Controller {

  get("/hey") {
    request => render.plain("hello").toFuture
  }

}

class FinatraServerSpec extends FlatSpecHelper {

  val app = new TestApp

  "app" should "register" in {
    val server = new FinatraServer
    server.register(app)

    new Runnable {
      def run() = server.start()
    }

    get("/hey")
    response.body should equal("hello")

    server.stop()

  }

  "app" should "add Filter" in {

    class FinatraServerMock extends FinatraServer with ShouldMatchers {
      override def allFilters(baseService: Service[FinagleRequest, FinagleResponse]):
        Service[FinagleRequest, FinagleResponse] = {

          filters should have length(1) // that is the LoggingFilter added below

          super.allFilters(baseService)
      }
    }

    val server = new FinatraServerMock()

    server.addFilter(new LoggingFilter)
    server.allFilters(new AppService(server.controllers))
  }

}
