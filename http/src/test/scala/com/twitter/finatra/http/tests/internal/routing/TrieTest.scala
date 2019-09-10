package com.twitter.finatra.http.tests.internal.routing

import com.twitter.finagle.Filter
import com.twitter.finatra.http.AnyMethod
import com.twitter.finagle.http.{Method, Request, Response}
import com.twitter.finatra.http.exceptions.MethodNotAllowedException
import com.twitter.finatra.http.internal.routing.{Route, Trie}
import com.twitter.inject.Test
import com.twitter.util.Future
import scala.reflect.classTag

class TrieTest extends Test {
  val routes: Seq[Route] = Seq(
    route(Method.Get, "/user/:id"),
    route(Method.Get, "/user/:id/:file.csv"),
    route(AnyMethod, "/tweets/:id"),
    route(Method.Get, "/tweets/user/list.:format"),
    route(Method.Delete, "/dms/:id/?"),
    route(Method.Get, "/:*")
  )
  val trie = new Trie(routes)

  test("Look up GET /user/1234 should be defined") {
    val route = trie.find("/user/1234", Method.Get).get
    val routePath = route.route.path
    val routeParams = route.routeParams

    routePath should be("/user/:id")
    routeParams.size should be(1)
    routeParams.get("id").get should be("1234")
  }

  test("Look up PUT /user/123 should be return a MethodNotAllowed exception") {
    intercept[MethodNotAllowedException] {
      trie.find("/user/123", Method.Put)
    }
  }

  test("Look up GET /user/1234/list.csv should be defined") {
    val route = trie.find("/user/1234/list.csv", Method.Get).get
    val routePath = route.route.path
    val routeParams = route.routeParams

    routePath should be("/user/:id/:file.csv")
    routeParams.size should be(2)
    routeParams.get("id").get should be("1234")
    routeParams.get("file").get should be("list")
  }

  test("Look up GET /tweets/user/list.txt should be defined") {
    val route = trie.find("/tweets/user/list.txt", Method.Get).get
    val routePath = route.route.path
    val routeParams = route.routeParams

    routePath should be("/tweets/user/list.:format")
    routeParams.size should be(1)
    routeParams.get("format").get should be("txt")
  }

  // AnyMethod should match any method names
  test("Look up Trace /tweets/1234 should be defined") {
    val route = trie.find("/tweets/1234", Method.Trace).get
    val routePath = route.route.path
    val routeParams = route.routeParams

    routePath should be("/tweets/:id")
    routeParams.size should be(1)
    routeParams.get("id").get should be("1234")
  }

  // :* should match everything in that segment
  test("GET /foo/bar should be defined") {
    val route = trie.find("/foo/bar", Method.Get).get
    val routePath = route.route.path
    val routeParams = route.routeParams

    routePath should be("/:*")
    routeParams.size should be(1)
    routeParams.get("*").get should be("foo/bar")
  }

  test("optional trailing slash identifier should match paths with or without trailing slash ") {
    val routeWithSlash = trie.find("/dms/1234", Method.Delete).get
    val routeWithoutSlash = trie.find("/dms/1234/", Method.Delete).get

    routeWithSlash.route.path should be("/dms/:id/")
    routeWithSlash.routeParams.size should be(1)
    routeWithSlash.routeParams.get("id").get should be("1234")

    routeWithoutSlash.route.path should be("/dms/:id/")
    routeWithoutSlash.routeParams.size should be(1)
    routeWithoutSlash.routeParams.get("id").get should be("1234")
  }

  /* ---------------------------------------------------------------- *
   *                             Utils                                *
   * ---------------------------------------------------------------- */
  protected def defaultCallback(request: Request): Future[Response] =
    Future.value(Response())

  def route(method: Method, path: String) =
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
