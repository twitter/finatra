package com.twitter.finatra.test

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.jboss.netty.util.CharsetUtil.UTF_8

import com.twitter.finatra.Response
import com.twitter.finatra_views.View

class MockView(val title:String) extends View {
  val template = "mock.mustache"
}

@RunWith(classOf[JUnitRunner])
class ResponseSpec extends ShouldSpec {
  def resp  = new Response
  def view        = new MockView("howdy view")

  ".ok" should "return a 200 response" in {
    resp.ok.status should equal (200)
  }

  ".notFound" should "return a 404 response" in {
    resp.notFound.status should equal (404)
  }

  ".status(201)" should "return a 201 response" in {
    resp.status(201).status should equal (201)
  }

  ".plain()" should "return a 200 plain response" in {
    val response = resp.plain("howdy")

    response.status should equal (200)
    response.strBody.get should equal ("howdy")
    response.headers("Content-Type") should equal ("text/plain")
  }

  ".html()" should "return a 200 html response" in {
    val response = resp.html("<h1>howdy</h1>")

    response.status should equal (200)
    response.strBody.get should equal ("<h1>howdy</h1>")
    response.headers("Content-Type") should equal ("text/html")
  }

  ".json()" should "return a 200 json response" in {
    val response = resp.json(Map("foo" -> "bar"))
    val body     = response.build.get.getContent.toString(UTF_8)

    response.status should equal (200)
    body should equal ("""{"foo":"bar"}""")
    response.headers("Content-Type") should equal ("application/json")
  }

  ".view()" should "return a 200 view response" in {
    val response = resp.view(view)
    val body     = response.build.get.getContent.toString(UTF_8)

    response.status should equal (200)
    body should include ("howdy view")
  }
}
