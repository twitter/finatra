package com.twitter.finatra.http.tests.integration.darktraffic.main

import com.twitter.finagle.http.Request
import com.twitter.finatra.http.Controller
import com.twitter.finatra.jackson.ScalaObjectMapper
import javax.inject.Inject

case class Foo(name: String)

class DarkTrafficTestController @Inject() (objectMapper: ScalaObjectMapper) extends Controller {
  get("/plaintext") { _: Request =>
    "Hello, World!"
  }

  put("/echo") { request: Request =>
    response.ok(request.contentString)
  }

  post("/foo") { foo: Foo =>
    foo.name
  }

  post("/bar") { request: Request =>
    objectMapper.parse[Foo](request.getContentString())
  }

  delete("/delete") { _: Request =>
    "delete"
  }
}
