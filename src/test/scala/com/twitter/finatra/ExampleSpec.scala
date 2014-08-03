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

import com.twitter.finatra._
import com.twitter.finatra.ContentType._

/* This test is used as the base for generating the
 README.markdown, all new generated apps, and the finatra_example repo
 */

class ExampleSpec extends FlatSpecHelper {

  /* ###BEGIN_APP### */

  class ExampleApp extends Controller {

    /**
     * Basic Example
     *
     * curl http://localhost:7070/ => "hello world"
     */
    get("/") { request =>
      render.static("index.html").toFuture
    }

    delete("/photos") { request =>
      render.plain("deleted!").toFuture
    }

    /**
     * Route parameters
     *
     * curl http://localhost:7070/user/dave => "hello dave"
     */
    get("/user/:username") { request =>
      val username = request.routeParams.getOrElse("username", "default_user")
      render.plain("hello " + username).toFuture
    }

    /**
     * Setting Headers
     *
     * curl -I http://localhost:7070/headers => "Foo:Bar"
     */
    get("/headers") { request =>
      render.plain("look at headers").header("Foo", "Bar").toFuture
    }

    /**
     * Rendering json
     *
     * curl -I http://localhost:7070/data.json => "{foo:bar}"
     */
    get("/data.json") { request =>
      render.json(Map("foo" -> "bar")).toFuture
    }

    /**
     * Query params
     *
     * curl http://localhost:7070/search?q=foo => "no results for foo"
     */
    get("/search") { request =>
      request.params.get("q") match {
        case Some(q) => render.plain("no results for "+ q).toFuture
        case None    => render.plain("query param q needed").status(500).toFuture
      }
    }

    /**
     * Redirects
     *
     * curl http://localhost:7070/redirect
     */
    get("/redirect") { request =>
      redirect("http://localhost:7070/", permanent = true).toFuture
    }

    /**
     * Uploading files
     *
     * curl -F avatar=@/path/to/img http://localhost:7070/profile
     */
    post("/profile") { request =>
      request.multiParams.get("avatar").map { avatar =>
        println("content type is " + avatar.contentType)
        avatar.writeToFile("/tmp/avatar") //writes uploaded avatar to /tmp/avatar
      }
      render.plain("ok").toFuture
    }

    options("/some/resource") { request =>
      render.plain("usage description").toFuture
    }

    /**
     * Rendering views
     *
     * curl http://localhost:7070/template
     */
    class AnView extends View {
      val template = "an_view.mustache"
      val some_val = "random value here"
    }

    get("/template") { request =>
      val anView = new AnView
      render.view(anView).toFuture
    }


    /**
     * Custom Error Handling
     *
     * curl http://localhost:7070/error
     */
    get("/error")   { request =>
      1234/0
      render.plain("we never make it here").toFuture
    }

    /**
     * Custom Error Handling with custom Exception
     *
     * curl http://localhost:7070/unauthorized
     */
    class Unauthorized extends Exception

    get("/unauthorized") { request =>
      throw new Unauthorized
    }

    error { request =>
      request.error match {
        case Some(e:ArithmeticException) =>
          render.status(500).plain("whoops, divide by zero!").toFuture
        case Some(e:Unauthorized) =>
          render.status(401).plain("Not Authorized!").toFuture
        case Some(e:UnsupportedMediaType) =>
          render.status(415).plain("Unsupported Media Type!").toFuture
        case _ =>
          render.status(500).plain("Something went wrong!").toFuture
      }
    }


    /**
     * Custom 404s
     *
     * curl http://localhost:7070/notfound
     */
    notFound { request =>
      render.status(404).plain("not found yo").toFuture
    }

    /**
     * Arbitrary Dispatch
     *
     * curl http://localhost:7070/go_home
     */
    get("/go_home") { request =>
      route.get("/")
    }

    get("/search_for_dogs") { request =>
      route.get("/search", Map("q" -> "dogs"))
    }

    get("/delete_photos") { request =>
      route.delete("/photos")
    }

    get("/gif") { request =>
      render.static("/dealwithit.gif").toFuture
    }

    get("/missing-gif") { request =>
      render.static("/dontdealwithit.gif").toFuture
    }

    /**
     * Dispatch based on Content-Type
     *
     * curl http://localhost:7070/blog/index.json
     * curl http://localhost:7070/blog/index.html
     */
    get("/blog/index.:format") { request =>
      respondTo(request) {
        case _:Html => render.html("<h1>Hello</h1>").toFuture
        case _:Json => render.json(Map("value" -> "hello")).toFuture
      }
    }

    /**
     * Also works without :format route using browser Accept header
     *
     * curl -H "Accept: text/html" http://localhost:7070/another/page
     * curl -H "Accept: application/json" http://localhost:7070/another/page
     * curl -H "Accept: foo/bar" http://localhost:7070/another/page
     */

    get("/another/page") { request =>
      respondTo(request) {
        case _:Html => render.plain("an html response").toFuture
        case _:Json => render.plain("an json response").toFuture
        case _:All => render.plain("default fallback response").toFuture
      }
    }

    /**
     * Metrics are supported out of the box via Twitter's Ostrich library.
     * More details here: https://github.com/twitter/ostrich
     *
     * curl http://localhost:7070/slow_thing
     *
     * By default a stats server is started on 9990:
     *
     * curl http://localhost:9990/stats.txt
     *
     */

    get("/slow_thing") { request =>
      stats.counter("slow_thing").incr
      stats.time("slow_thing time") {
        Thread.sleep(100)
      }
      render.plain("slow").toFuture
    }

  }

  /* ###END_APP### */

  val server = new FinatraServer
  server.register(new ExampleApp)

  /* ###BEGIN_SPEC### */

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

  "GET /index.html" should "respond 200" in {
    get("/")
    response.body.contains("Finatra - The scala web framework") should equal(true)
    response.code should equal(200)
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

  "GET /redirect" should "respond with /" in {
    get("/redirect")
    response.body should equal("Redirecting to <a href=\"http://localhost:7070/\">http://localhost:7070/</a>.")
    response.code should equal(301)
  }

  "OPTIONS /some/resource" should "respond with usage description" in {
    options("/some/resource")
    response.body should equal("usage description")
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

  "GET /go_home" should "render same as /" in {
    get("/go_home")
    response.body.contains("Finatra - The scala web framework") should equal(true)
    response.code should equal(200)
  }

  "GET /search_for_dogs" should "render same as /search?q=dogs" in {
    get("/search_for_dogs")
    response.code should equal(200)
    response.body should equal("no results for dogs")
  }

  "GET /delete_photos" should "render same as DELETE /photos" in {
    get("/delete_photos")
    response.code should equal(200)
    response.body should equal("deleted!")
  }

  "GET /gif" should "render dealwithit.gif" in {
    get("/gif")
    response.code should equal(200)
    response.originalResponse.getContent().array().head should equal(71) // capital "G", detects the gif
  }

  "GET /missing-gif" should "return a 404" in {
    get("/missing-gif")
    response.code should equal(404)
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

  /* ###END_SPEC### */
}
