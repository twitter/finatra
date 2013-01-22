package ###PACKAGE_NAME###

import org.scalatest.FlatSpec
import org.scalatest.matchers.ShouldMatchers
import com.twitter.finatra.test._
import ###PACKAGE_NAME###._

class AppSpec extends SpecHelper {

  val app = new App.ExampleApp

  
  "GET /notfound" should "respond 404" in {
    get("/notfound")
    response.body   should equal ("not found yo")
    response.code   should equal (404)
  }

  "GET /error" should "respond 500" in {
    get("/error")
    response.body   should equal ("whoops, divide by zero!")
    response.code   should equal (500)
  }

  "GET /unauthorized" should "respond 401" in {
    get("/unauthorized")
    response.body   should equal ("Not Authorized!")
    response.code   should equal (401)
  }

  "GET /hello" should "respond with hello world" in {
    get("/")
    response.body should equal ("hello world")
  }

  "GET /user/foo" should "responsd with hello foo" in {
    get("/user/foo")
    response.body should equal ("hello foo")
  }

  "GET /headers" should "respond with Foo:Bar" in {
    get("/headers")
    response.getHeader("Foo") should equal("Bar")
  }

  "GET /data.json" should """respond with {"foo":"bar"}""" in {
    get("/data.json")
    response.body should equal("""{"foo":"bar"}""")
  }

  "GET /search?q=foo" should "respond with no results for foo" in {
    get("/search?q=foo")
    response.body should equal("no results for foo")
  }

  "GET /template" should "respond with a rendered template" in {
    get("/template")
    response.body should equal("Your value is random value here")
  }

  "GET /blog/index.json" should "should have json" in {
    get("/blog/index.json")
    response.body should equal("""{"value":"hello"}""")
  }

  "GET /blog/index.html" should "should have html" in {
    get("/blog/index.html")
    response.body should equal("""<h1>Hello</h1>""")
  }

  "GET /blog/index.rss" should "respond in a 415" in {
    get("/blog/index.rss")
    response.code should equal(415)
  }

  "GET /another/page with html" should "respond with html" in {
    get("/another/page", Map.empty, Map("Accept" -> "text/html"))
    response.body should equal("an html response")
  }

  "GET /another/page with json" should "respond with json" in {
    get("/another/page", Map.empty, Map("Accept" -> "application/json"))
    response.body should equal("an json response")
  }

  "GET /another/page with unsupported type" should "respond with catch all" in {
    get("/another/page", Map.empty, Map("Accept" -> "foo/bar"))
    response.body should equal("default fallback response")
  }

}
