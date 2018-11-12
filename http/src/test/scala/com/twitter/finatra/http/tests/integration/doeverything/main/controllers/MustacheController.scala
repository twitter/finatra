package com.twitter.finatra.http.tests.integration.doeverything.main.controllers

import com.twitter.finagle.http.{MediaType, Request}
import com.twitter.finatra.http.Controller
import com.twitter.finatra.http.response.Mustache

@Mustache("testHtml")
case class HTMLView(age: Int, name: String, friends: Seq[String])

@Mustache(
  value = "testJson",
  contentType = "application/json; charset=utf-8"
)
case class JSONView(name: String)

case class NoAnnotation(name: String)

class MustacheController extends Controller {
  get("/mustache.html") { _: Request =>
    HTMLView(age = 42, name = "HTMel", friends = Seq.empty[String])
  }

  get("/mustache.json") { _: Request =>
    JSONView("JSONDay")
  }

  get("/mustache-view-before.json") { _: Request =>
    response
      .ok
      .contentType(MediaType.Json)
      .view(
        "testJson.mustache",
        JSONView(name = "JSONDay")
      )
  }

  get("/mustache-view-after.json") { _: Request =>
    response
      .ok
      .view(
        "testJson.mustache",
        JSONView("JSONDay")
      )
      .contentType(MediaType.Json)
  }

  get("/mustache-use-annotation-template-name.json") { _: Request =>
    response
      .ok
      .contentType(MediaType.Json)
      .view(JSONView("JSONDay"))
  }

  /*
    Actually returns 500 because there's no way to find a template name.
   */
  get("/mustache-view-without-template-name-or-annotation.json") { _: Request =>
    response
      .ok
      .contentType(MediaType.Json)
      .view(NoAnnotation("NoAnnotation"))
  }
}