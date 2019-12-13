package com.twitter.finatra.http.tests

import com.twitter.finagle.http.Status.{Created, InternalServerError, Ok}
import com.twitter.finatra.http.EmbeddedHttpServer
import com.twitter.finatra.httpclient.RequestBuilder
import com.twitter.finatra.http.tests.server.TestHttpServer
import com.twitter.inject.server.FeatureTest

class MustacheModuleFeatureTest extends FeatureTest {

  override val server: EmbeddedHttpServer = new EmbeddedHttpServer(
    twitterServer = new TestHttpServer,
    disableTestLogging = true
  )

  test("POST /formPostView") {
    server.httpFormPost(
      "/formPostView",
      params = Map("name" -> "bob", "age" -> "18"),
      andExpect = Ok,
      withBody = "age:18\nname:bob\nuser1\nuser2\n"
    )
  }

  test("POST /formPostViewFromBuilderView (from BuilderView with diff template than annotation)") {
    server.httpFormPost(
      "/formPostViewFromBuilderView",
      params = Map("name" -> "bob", "age" -> "18"),
      andExpect = Ok,
      withBody = "age2:18\nname2:bob\nuser1\nuser2\n"
    )
  }

  test("POST /formPostViewFromBuilderHtml") {
    server.httpFormPost(
      "/formPostViewFromBuilderHtml",
      params = Map("name" -> "bob", "age" -> "18"),
      andExpect = Ok,
      withBody = "age:18\nname:bob\nuser1\nuser2\n"
    )
  }

  test("POST /formPostViewFromBuilderCreatedView") {
    val response = server.httpFormPost(
      "/formPostViewFromBuilderCreatedView",
      params = Map("name" -> "bob", "age" -> "18"),
      andExpect = Created,
      withBody = "age2:18\nname2:bob\nuser1\nuser2\n"
    )

    response.location should equal(Some(server.fullHttpURI("/foo/1")))
  }

  test("POST /formPostViewFromBuilderCreatedHtml") {
    val response = server.httpFormPost(
      "/formPostViewFromBuilderCreatedHtml",
      params = Map("name" -> "bob", "age" -> "18"),
      andExpect = Created,
      withBody = "age:18\nname:bob\nuser1\nuser2\n"
    )

    response.location should equal(Some(server.fullHttpURI("/foo/1")))
  }

  test("GET /getView") {
    server.httpGet(
      "/getView?age=18&name=bob",
      andExpect = Ok,
      withBody = "age:18\nname:bob\nuser1\nuser2\n"
    )
  }

  test("GET /testClassWithHtml") {
    server.httpGet(
      "/testClassWithHtml",
      andExpect = Ok,
      withJsonBody =
        """
          |{
          |  "address" : "123 Main St. Anywhere, CA US 90210",
          |  "phone" : "+12221234567",
          |  "rendered_html" : "&lt;div class=&quot;nav&quot;&gt;\n  &lt;table cellpadding=&quot;0&quot; cellspacing=&quot;0&quot;&gt;\n    &lt;tr&gt;\n        &lt;th&gt;Name&lt;/th&gt;\n        &lt;th&gt;Age&lt;/th&gt;\n        &lt;th&gt;Friends&lt;/th&gt;\n    &lt;/tr&gt;\n    &lt;tr&gt;\n        &lt;td&gt;age2:28&lt;/td&gt;\n        &lt;td&gt;name:Bob Smith&lt;/td&gt;\n        &lt;td&gt;\n            user1\n            user2\n        &lt;/td&gt;\n    &lt;/tr&gt;\n  &lt;/table&gt;\n&lt;/div&gt;"
          |}
        """.stripMargin
    )
  }

  test("GET /mustache.html") {
    val response = server.httpRequest(
      request = RequestBuilder.get("/mustache.html"),
      andExpect = Ok,
      withBody = """<div class="nav">
                   |  <table cellpadding="0" cellspacing="0">
                   |    <tr>
                   |        <th>Name</th>
                   |        <th>Age</th>
                   |        <th>Friends</th>
                   |    </tr>
                   |    <tr>
                   |        <td>age2:42</td>
                   |        <td>name:HTMel</td>
                   |        <td>
                   |        </td>
                   |    </tr>
                   |  </table>
                   |</div>""".stripMargin
    )

    response.contentType should be(Some("text/html; charset=utf-8"))
  }

  test("GET /mustache.json") {
    val response = server.httpRequest(
      request = RequestBuilder.get("/mustache.json"),
      andExpect = Ok,
      withJsonBody = """{"name":"JSONDay"}"""
    )

    response.contentType should be(Some("application/json; charset=utf-8"))
  }

  test("GET /mustache-view-before.json") {
    val response = server.httpRequest(
      request = RequestBuilder.get("/mustache-view-before.json"),
      andExpect = Ok,
      withJsonBody = """{"name":"JSONDay"}"""
    )

    response.contentType should be(Some("application/json; charset=utf-8"))
  }

  test("GET /mustache-view-after.json") {
    val response = server.httpRequest(
      request = RequestBuilder.get("/mustache-view-after.json"),
      andExpect = Ok,
      withJsonBody = """{"name":"JSONDay"}"""
    )

    response.contentType should be(Some("application/json; charset=utf-8"))
  }

  test("GET /mustache-use-annotation-template-name.json") {
    val response = server.httpRequest(
      request = RequestBuilder.get("/mustache-use-annotation-template-name.json"),
      andExpect = Ok,
      withJsonBody = """{"name":"JSONDay"}"""
    )

    response.contentType should be(Some("application/json; charset=utf-8"))
  }

  test("GET /mustache-view-without-template-name-or-annotation.json") {
    val response = server.httpRequest(
      request = RequestBuilder.get("/mustache-view-without-template-name-or-annotation.json"),
      andExpect = InternalServerError,
      withJsonBody = """{"errors":["internal server error"]}"""
    )

    response.contentType should be(Some("application/json; charset=utf-8"))
  }
}
