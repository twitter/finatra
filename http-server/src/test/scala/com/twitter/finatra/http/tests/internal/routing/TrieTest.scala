package com.twitter.finatra.http.tests.internal.routing

import com.twitter.finagle.Filter
import com.twitter.finagle.http.Method
import com.twitter.finagle.http.Request
import com.twitter.finagle.http.Response
import com.twitter.finatra.http.exceptions.UnsupportedMethodException
import com.twitter.finatra.http.internal.routing.Route
import com.twitter.finatra.http.internal.routing.Trie
import com.twitter.finatra.http.request.AnyMethod
import com.twitter.inject.Test
import com.twitter.util.Future
import scala.reflect.classTag

class TrieTest extends Test {
  val routes: Seq[Route] = Seq(
    route(Method.Get, "/user/:id"),
    route(Method.Get, "/user/:id/"),
    route(Method.Get, "/user/:id/:file.csv"),
    route(AnyMethod, "/tweets/:id"),
    route(Method.Get, "/tweets/user/list.:format"),
    route(Method.Delete, "/dms/:id/?"),
    // a non-constant route ends with constant segment
    route(Method.Get, "/dms/:id/get/?"),
    route(Method.Get, "/:*")
  )
  val trie = new Trie(routes)

  test("Look up GET /user/1234 should be defined") {
    val route = trie.find("/user/1234", Method.Get).get
    val routePath = route.route.path
    val routeParams = route.routeParams

    routePath should be("/user/:id")
    routeParams.size should be(1)
    routeParams("id") should be("1234")
  }

  test("Look up GET /user/1234/ should be defined") {
    val route = trie.find("/user/1234/", Method.Get).get
    val routePath = route.route.path
    val routeParams = route.routeParams

    routePath should be("/user/:id/")
    routeParams.size should be(1)
    routeParams("id") should be("1234")
  }

  test("Look up PUT /user/123 should be return a UnsupportedMethodException") {
    intercept[UnsupportedMethodException] {
      trie.find("/user/123", Method.Put)
    }
  }

  test("Look up GET /user/1234/list.csv should be defined") {
    val route = trie.find("/user/1234/list.csv", Method.Get).get
    val routePath = route.route.path
    val routeParams = route.routeParams

    routePath should be("/user/:id/:file.csv")
    routeParams.size should be(2)
    routeParams("id") should be("1234")
    routeParams("file") should be("list")
  }

  test("Look up GET /tweets/user/list.txt should be defined") {
    val route = trie.find("/tweets/user/list.txt", Method.Get).get
    val routePath = route.route.path
    val routeParams = route.routeParams

    routePath should be("/tweets/user/list.:format")
    routeParams.size should be(1)
    routeParams("format") should be("txt")
  }

  // AnyMethod should match any method names
  test("Look up Trace /tweets/1234 should be defined") {
    val route = trie.find("/tweets/1234", Method.Trace).get
    val routePath = route.route.path
    val routeParams = route.routeParams

    routePath should be("/tweets/:id")
    routeParams.size should be(1)
    routeParams("id") should be("1234")
  }
  // treat routes with the same segments but different trailing slash variant as different routes
  test("Look up Trace /tweets/1234/ should not be defined") {
    // define a route without `/:*`
    val routes: Seq[Route] = Seq(
      route(AnyMethod, "/tweets/:id")
    )
    val trie = new Trie(routes)

    val foundRoute = trie.find("/tweets/1234", Method.Trace).get
    val routePath = foundRoute.route.path
    val routeParams = foundRoute.routeParams
    routePath should be("/tweets/:id")
    routeParams.size should be(1)
    routeParams("id") should be("1234")
    // a path with trailing slash is not found
    trie.find("/tweets/1234/", Method.Trace) should be(None)
  }

  // :* should match everything in that segment
  test("GET /foo/bar should be defined") {
    val route = trie.find("/foo/bar", Method.Get).get
    val routePath = route.route.path
    val routeParams = route.routeParams

    routePath should be("/:*")
    routeParams.size should be(1)
    routeParams("*") should be("foo/bar")
  }

  test("optional trailing slash identifier should match paths with or without trailing slash ") {
    val routeWithSlash = trie.find("/dms/1234", Method.Delete).get
    val routeWithoutSlash = trie.find("/dms/1234/", Method.Delete).get

    routeWithSlash.route.path should be("/dms/:id/")
    routeWithSlash.routeParams.size should be(1)
    routeWithSlash.routeParams("id") should be("1234")

    routeWithoutSlash.route.path should be("/dms/:id/")
    routeWithoutSlash.routeParams.size should be(1)
    routeWithoutSlash.routeParams("id") should be("1234")
  }

  test(
    "optional trailing slash identifier should match paths with or without trailing slash for routes with constant segment") {
    val routeWithSlash = trie.find("/dms/123/get", Method.Get).get
    val routeWithoutSlash = trie.find("/dms/123/get/", Method.Get).get

    routeWithSlash.route.path should be("/dms/:id/get/")
    routeWithSlash.routeParams.size should be(1)
    routeWithSlash.routeParams("id") should be("123")

    routeWithoutSlash.route.path should be("/dms/:id/get/")
    routeWithoutSlash.routeParams.size should be(1)
    routeWithoutSlash.routeParams("id") should be("123")
  }

  /* ---------------------------------------------------------------- *
   *                             Utils                                *
   * ---------------------------------------------------------------- */
  protected def defaultCallback(request: Request): Future[Response] =
    Future.value(request).map(_ => Response())

  def route(method: Method, path: String): Route =
    Route(
      name = path,
      method = method,
      uri = path,
      clazz = this.getClass,
      admin = false,
      index = None,
      callback = defaultCallback,
      annotations = Seq(),
      requestClass = classTag[Request],
      responseClass = classTag[Response],
      routeFilter = Filter.identity,
      filter = Filter.identity
    )
}
