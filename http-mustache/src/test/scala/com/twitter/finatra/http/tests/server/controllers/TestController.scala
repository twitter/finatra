package com.twitter.finatra.http.tests.server.controllers

import com.twitter.finagle.http.{MediaType, Request}
import com.twitter.finatra.http.Controller
import com.twitter.finatra.http.marshalling.response._
import com.twitter.finatra.http.tests.server.domain.{FormPostRequest, TestCaseClassWithHtml}
import com.twitter.finatra.http.tests.server.views.{HTMLView, JSONView, NoAnnotation, TestUserView}
import com.twitter.finatra.mustache.marshalling.MustacheService
import javax.inject.Inject

class TestController @Inject() (mustacheService: MustacheService) extends Controller {

  post("/formPostView") { formPost: FormPostRequest =>
    TestUserView(formPost.age, formPost.name, Seq("user1", "user2"))
  }

  post("/formPostViewFromBuilderView") { formPost: FormPostRequest =>
    response.ok
      .view("testuser2.mustache", TestUserView(formPost.age, formPost.name, Seq("user1", "user2")))
  }

  post("/formPostViewFromBuilderHtml") { formPost: FormPostRequest =>
    response.ok.html(TestUserView(formPost.age, formPost.name, Seq("user1", "user2")))
  }

  post("/formPostViewFromBuilderCreatedView") { formPost: FormPostRequest =>
    response.created
      .location("/foo/1")
      .view("testuser2.mustache", TestUserView(formPost.age, formPost.name, Seq("user1", "user2")))
  }

  post("/formPostViewFromBuilderCreatedHtml") { formPost: FormPostRequest =>
    response.created
      .location("/foo/1")
      .html(TestUserView(formPost.age, formPost.name, Seq("user1", "user2")))
  }

  get("/getView") { request: Request =>
    TestUserView(request.params.getInt("age").get, request.params("name"), Seq("user1", "user2"))
  }

  get("/testClassWithHtml") { _: Request =>
    val testUser = TestUserView(28, "Bob Smith", Seq("user1", "user2"))

    TestCaseClassWithHtml(
      address = "123 Main St. Anywhere, CA US 90210",
      phone = "+12221234567",
      renderedHtml = xml.Utility.escape(mustacheService.createString("testHtml.mustache", testUser))
    )
  }

  get("/mustache.html") { _: Request =>
    HTMLView(age = 42, name = "HTMel", friends = Seq.empty[String])
  }

  get("/mustache.json") { _: Request =>
    JSONView("JSONDay")
  }

  get("/mustache-view-before.json") { _: Request =>
    response.ok
      .contentType(MediaType.Json)
      .view(
        "testJson.mustache",
        JSONView(name = "JSONDay")
      )
  }

  get("/mustache-view-after.json") { _: Request =>
    response.ok
      .view(
        "testJson.mustache",
        JSONView("JSONDay")
      )
      .contentType(MediaType.Json)
  }

  get("/mustache-use-annotation-template-name.json") { _: Request =>
    response.ok
      .contentType(MediaType.Json)
      .view(JSONView("JSONDay"))
  }

  /*
    Actually returns 500 because there's no way to find a template name.
   */
  get("/mustache-view-without-template-name-or-annotation.json") { _: Request =>
    response.ok
      .contentType(MediaType.Json)
      .view(NoAnnotation("NoAnnotation"))
  }
}
